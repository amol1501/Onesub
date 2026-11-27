#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 4 ]; then
    echo "Usage: upload-manual-tls-bundle.sh KEY_VAULT_NAME DOMAIN FULLCHAIN_PEM PRIVATE_KEY_PEM" >&2
    exit 2
fi

vault_name="$1"
domain="$2"
fullchain_source="$3"
private_key_source="$4"

for source_file in "$fullchain_source" "$private_key_source"; do
    if [ ! -r "$source_file" ]; then
        echo "Cannot read TLS input file: $source_file" >&2
        exit 1
    fi
done

umask 077
temporary_directory="$(mktemp -d "${TMPDIR:-/tmp}/onesub-tls-upload.XXXXXX")"
trap 'rm -rf "$temporary_directory"' EXIT

fullchain_file="$temporary_directory/fullchain.pem"
private_key_file="$temporary_directory/privkey.pem"
bundle_file="$temporary_directory/onesub-tls-bundle.json"
cp "$fullchain_source" "$fullchain_file"
cp "$private_key_source" "$private_key_file"

python3 - "$fullchain_file" "$private_key_file" "$bundle_file" <<'PY'
import json
import os
import sys
from pathlib import Path

fullchain, private_key, output = map(Path, sys.argv[1:])
bundle = {
    "fullchain": fullchain.read_text(encoding="utf-8"),
    "privateKey": private_key.read_text(encoding="utf-8"),
}
output.write_text(json.dumps(bundle), encoding="utf-8")
output.chmod(0o600)
PY

python3 "$(dirname "$0")/validate-manual-tls-bundle.py" "$bundle_file" "$domain"
az keyvault secret set \
    --vault-name "$vault_name" \
    --name onesub-tls-bundle \
    --file "$bundle_file" \
    --only-show-errors \
    --output none

echo "Uploaded and validated the manual TLS bundle in Key Vault: $vault_name"
