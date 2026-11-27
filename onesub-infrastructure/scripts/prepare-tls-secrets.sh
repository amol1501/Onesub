#!/usr/bin/env bash
set -euo pipefail

: "${GODADDY_API_KEY:?Set GODADDY_API_KEY from a Jenkins secret credential}"
: "${GODADDY_API_SECRET:?Set GODADDY_API_SECRET from a Jenkins secret credential}"
: "${ONESUB_DOMAIN:?Set ONESUB_DOMAIN}"
: "${ONESUB_FRONTEND_KEY_VAULT:?Set ONESUB_FRONTEND_KEY_VAULT}"
: "${WORKSPACE:?Set WORKSPACE}"
: "${HOME:?HOME must be set}"

ACME_HOME="${ACME_HOME:-$HOME/.onesub-acme}"
mkdir -p "$ACME_HOME"
chmod 700 "$ACME_HOME"

lego_version="4.22.2"
release_url="https://github.com/go-acme/lego/releases/download/v$lego_version"
temporary_directory="$(mktemp -d)"
trap 'rm -rf "$temporary_directory"' EXIT

case "$(uname -s)" in
    Darwin) host_os="darwin" ;;
    Linux) host_os="linux" ;;
    *)
        echo "Unsupported Jenkins host operating system: $(uname -s)" >&2
        exit 1
        ;;
esac
case "$(uname -m)" in
    arm64|aarch64) host_arch="arm64" ;;
    x86_64|amd64) host_arch="amd64" ;;
    *)
        echo "Unsupported Jenkins host architecture: $(uname -m)" >&2
        exit 1
        ;;
esac

download_lego() {
    local os="$1"
    local arch="$2"
    local asset="lego_v${lego_version}_${os}_${arch}.tar.gz"
    curl --fail --location --silent --show-error \
        "$release_url/$asset" \
        --output "$temporary_directory/$asset"
    curl --fail --location --silent --show-error \
        "$release_url/lego_${lego_version}_checksums.txt" \
        --output "$temporary_directory/checksums.txt"
    local expected
    expected="$(awk -v asset="$asset" '$2 == asset { print $1 }' "$temporary_directory/checksums.txt")"
    [ -n "$expected" ] || {
        echo "No published checksum found for $asset." >&2
        exit 1
    }
    local actual
    actual="$(shasum -a 256 "$temporary_directory/$asset" | awk '{ print $1 }')"
    [ "$actual" = "$expected" ] || {
        echo "Checksum verification failed for $asset." >&2
        exit 1
    }
    mkdir -p "$temporary_directory/$os-$arch"
    tar -xzf "$temporary_directory/$asset" -C "$temporary_directory/$os-$arch" lego
    printf '%s\n' "$temporary_directory/$os-$arch/lego"
}

lego_host="$(download_lego "$host_os" "$host_arch")"
lego_linux="$(download_lego linux amd64)"
install -d -m 700 "$ACME_HOME/bin"
install -m 700 "$lego_host" "$ACME_HOME/bin/lego"
install -m 755 "$lego_linux" "$WORKSPACE/lego-linux-amd64"
LEGO_BIN="$ACME_HOME/bin/lego"

certificate="$ACME_HOME/certificates/$ONESUB_DOMAIN.crt"
private_key="$ACME_HOME/certificates/$ONESUB_DOMAIN.key"
if [ -s "$certificate" ] && [ -s "$private_key" ]; then
    "$LEGO_BIN" \
        --path "$ACME_HOME" \
        --dns godaddy \
        --domains "$ONESUB_DOMAIN" \
        --accept-tos \
        renew --days 30
else
    "$LEGO_BIN" \
        --path "$ACME_HOME" \
        --dns godaddy \
        --domains "$ONESUB_DOMAIN" \
        --accept-tos \
        run
fi

issuer="$ACME_HOME/certificates/$ONESUB_DOMAIN.issuer.crt"
[ -s "$certificate" ] && [ -s "$private_key" ] && [ -s "$issuer" ] || {
    echo "lego did not produce the expected certificate files." >&2
    exit 1
}

umask 077
python3 - "$certificate" "$issuer" "$private_key" "$temporary_directory/tls-bundle.json" <<'PY'
import json
import sys
from pathlib import Path

certificate, issuer, private_key, output = map(Path, sys.argv[1:])
bundle = {
    "fullchain": certificate.read_text(encoding="utf-8") + issuer.read_text(encoding="utf-8"),
    "privateKey": private_key.read_text(encoding="utf-8"),
}
output.write_text(json.dumps(bundle), encoding="utf-8")
output.chmod(0o600)
PY
printf '%s' "$GODADDY_API_KEY" > "$temporary_directory/godaddy-api-key"
printf '%s' "$GODADDY_API_SECRET" > "$temporary_directory/godaddy-api-secret"

set_secret() {
    local secret_name="$1"
    local secret_file="$2"
    local attempt=1
    while ! az keyvault secret set \
        --vault-name "$ONESUB_FRONTEND_KEY_VAULT" \
        --name "$secret_name" \
        --file "$secret_file" \
        --only-show-errors \
        --output none
    do
        if [ "$attempt" -ge 12 ]; then
            echo "Failed to write Key Vault secret: $secret_name" >&2
            return 1
        fi
        attempt=$((attempt + 1))
        sleep 10
    done
}

set_secret onesub-tls-bundle "$temporary_directory/tls-bundle.json"
set_secret godaddy-api-key "$temporary_directory/godaddy-api-key"
set_secret godaddy-api-secret "$temporary_directory/godaddy-api-secret"
