#!/usr/bin/env bash
set -euo pipefail

: "${ONESUB_BACKEND_KEY_VAULT:?Set ONESUB_BACKEND_KEY_VAULT}"
: "${ONESUB_DB_URL:?Set ONESUB_DB_URL}"
: "${ONESUB_DB_USERNAME:?Set ONESUB_DB_USERNAME}"
: "${ONESUB_DB_PASSWORD:?Set ONESUB_DB_PASSWORD}"

temporary_directory="$(mktemp -d)"
trap 'rm -rf "$temporary_directory"' EXIT
umask 077
printf '%s' "$ONESUB_DB_URL" > "$temporary_directory/db-url"
printf '%s' "$ONESUB_DB_USERNAME" > "$temporary_directory/db-username"
printf '%s' "$ONESUB_DB_PASSWORD" > "$temporary_directory/db-password"

set_secret() {
    local secret_name="$1"
    local secret_file="$2"
    local attempt=1
    while ! az keyvault secret set \
        --vault-name "$ONESUB_BACKEND_KEY_VAULT" \
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

for secret in db-url db-username db-password; do
    set_secret "$secret" "$temporary_directory/$secret"
done
