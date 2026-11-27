#!/usr/bin/env bash
set -euo pipefail

: "${ONESUB_STORAGE_ACCOUNT:?Set ONESUB_STORAGE_ACCOUNT}"
: "${ONESUB_STORAGE_CONTAINER:?Set ONESUB_STORAGE_CONTAINER}"
: "${ONESUB_RELEASE:?Set ONESUB_RELEASE}"
: "${ONESUB_BACKEND_JAR:?Set ONESUB_BACKEND_JAR}"
: "${WORKSPACE:?Set WORKSPACE}"

temporary_directory="$(mktemp -d "$WORKSPACE/.onesub-publish.XXXXXX")"
trap 'rm -rf "$temporary_directory"' EXIT

frontend_archive="$temporary_directory/onesub-frontend.tar.gz"
tar -czf "$frontend_archive" -C "$WORKSPACE/onesub-frontend/dist" .

upload_blob() {
    local name="$1"
    local file="$2"
    local attempt=1
    while ! az storage blob upload \
        --account-name "$ONESUB_STORAGE_ACCOUNT" \
        --container-name "$ONESUB_STORAGE_CONTAINER" \
        --name "$name" \
        --file "$file" \
        --auth-mode login \
        --overwrite \
        --only-show-errors \
        --output none
    do
        if [ "$attempt" -ge 6 ]; then
            echo "Failed to upload artifact blob: $name" >&2
            return 1
        fi
        attempt=$((attempt + 1))
        sleep 10
    done
}

upload_blob \
    "releases/frontend/$ONESUB_RELEASE.tar.gz" \
    "$frontend_archive"
upload_blob \
    "releases/backend/$ONESUB_RELEASE/onesub-backend.jar" \
    "$ONESUB_BACKEND_JAR"
upload_blob \
    "tools/lego-linux-amd64" \
    "$WORKSPACE/lego-linux-amd64"

manifest="$temporary_directory/onesub-current.json"
printf '{"release":"%s","frontend":"releases/frontend/%s.tar.gz","backend":"releases/backend/%s/onesub-backend.jar"}\n' \
    "$ONESUB_RELEASE" "$ONESUB_RELEASE" "$ONESUB_RELEASE" > "$manifest"
upload_blob "current.json" "$manifest"
