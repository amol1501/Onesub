#!/usr/bin/env python3

import json
import os
import re
import shutil
import subprocess
import sys
import tarfile
import tempfile
import urllib.error
from email.utils import formatdate
import urllib.parse
import urllib.request
from pathlib import Path


IMDS_TOKEN_URL = "http://169.254.169.254/metadata/identity/oauth2/token"
STORAGE_API_VERSION = "2023-11-03"
KEY_VAULT_API_VERSION = "7.4"


def access_token(resource):
    query = urllib.parse.urlencode({"api-version": "2018-02-01", "resource": resource})
    request = urllib.request.Request(
        f"{IMDS_TOKEN_URL}?{query}",
        headers={"Metadata": "true"},
    )
    with urllib.request.urlopen(request, timeout=15) as response:
        return json.load(response)["access_token"]


def read_url(url, token, headers=None):
    request_headers = {"Authorization": f"Bearer {token}"}
    request_headers.update(headers or {})
    request = urllib.request.Request(url, headers=request_headers)
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()


def get_secret(vault_name, secret_name):
    token = access_token("https://vault.azure.net")
    url = (
        f"https://{vault_name}.vault.azure.net/secrets/"
        f"{urllib.parse.quote(secret_name, safe='')}?api-version={KEY_VAULT_API_VERSION}"
    )
    return json.loads(read_url(url, token))["value"]


def get_blob(account_name, container_name, blob_name):
    token = access_token("https://storage.azure.com/")
    encoded_blob = urllib.parse.quote(blob_name, safe="/")
    url = f"https://{account_name}.blob.core.windows.net/{container_name}/{encoded_blob}"
    headers = {
        "x-ms-date": formatdate(usegmt=True),
        "x-ms-version": STORAGE_API_VERSION,
    }
    return read_url(url, token, headers)


def certificate_expires_within(fullchain, seconds):
    with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", suffix=".pem") as certificate_file:
        certificate_file.write(fullchain)
        certificate_file.flush()
        result = subprocess.run(
            ["openssl", "x509", "-in", certificate_file.name, "-noout", "-checkend", str(seconds)],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=False,
        )
    return result.returncode != 0


def certificate_matches_domain(fullchain, domain):
    with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", suffix=".pem") as certificate_file:
        certificate_file.write(fullchain)
        certificate_file.flush()
        result = subprocess.run(
            [
                "openssl",
                "x509",
                "-in",
                certificate_file.name,
                "-noout",
                "-checkhost",
                domain,
            ],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=False,
        )
    return result.returncode == 0


def ensure_tls_bundle(vault_name, domain):
    try:
        raw_bundle = get_secret(vault_name, "onesub-tls-bundle")
    except urllib.error.HTTPError as error:
        if error.code == 404:
            raise RuntimeError(
                "Key Vault secret onesub-tls-bundle is missing; upload the manual TLS bundle."
            ) from error
        raise
    bundle = json.loads(raw_bundle)
    if not isinstance(bundle, dict):
        raise ValueError("Key Vault secret onesub-tls-bundle must be a JSON object")
    fullchain = bundle.get("fullchain", "")
    private_key = bundle.get("privateKey", "")
    if not isinstance(fullchain, str) or "-----BEGIN CERTIFICATE-----" not in fullchain:
        raise ValueError("Key Vault TLS full chain is not PEM encoded")
    if not isinstance(private_key, str) or "PRIVATE KEY-----" not in private_key:
        raise ValueError("Key Vault TLS private key is not PEM encoded")
    if not certificate_matches_domain(fullchain, domain):
        raise ValueError(f"Key Vault TLS certificate does not cover {domain}")
    if certificate_expires_within(fullchain, 0):
        raise ValueError("Key Vault TLS certificate is expired; renew it manually.")
    if certificate_expires_within(fullchain, 30 * 24 * 60 * 60):
        print(
            "Warning: TLS certificate expires within 30 days; renew it manually and upload the new bundle.",
            file=sys.stderr,
        )
    return bundle


def safe_release_name(value):
    if not re.fullmatch(r"[A-Za-z0-9._-]{1,80}", value):
        raise ValueError(f"Invalid release identifier: {value!r}")
    return value


def safe_blob_name(value):
    if value.startswith("/") or ".." in Path(value).parts:
        raise ValueError(f"Invalid artifact blob path: {value!r}")
    return value


def extract_frontend(archive_data, destination):
    destination.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(prefix="onesub-frontend-", suffix=".tar.gz") as archive_file:
        archive_file.write(archive_data)
        archive_file.flush()
        with tarfile.open(archive_file.name, "r:gz") as archive:
            for member in archive.getmembers():
                member_path = (destination / member.name).resolve()
                if destination.resolve() not in member_path.parents and member_path != destination.resolve():
                    raise ValueError(f"Artifact contains an unsafe path: {member.name!r}")
                if member.issym() or member.islnk():
                    raise ValueError(f"Artifact contains an unsupported link: {member.name!r}")
                if not (member.isdir() or member.isfile()):
                    raise ValueError(f"Artifact contains an unsupported file type: {member.name!r}")
            archive.extractall(destination)
    if not (destination / "index.html").is_file():
        raise ValueError("Frontend artifact is missing index.html")


def write_secret_file(path, content, mode):
    temporary_path = path.with_suffix(path.suffix + ".tmp")
    file_descriptor = os.open(temporary_path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, mode)
    with os.fdopen(file_descriptor, "w", encoding="utf-8") as secret_file:
        secret_file.write(content)
        secret_file.flush()
        os.fsync(secret_file.fileno())
    temporary_path.replace(path)


def systemd_environment_value(value):
    if "\n" in value or "\r" in value:
        raise ValueError("Environment values cannot contain line breaks")
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def deploy_backend(manifest, release):
    artifact = safe_blob_name(manifest["backend"])
    release_dir = Path("/opt/onesub/releases") / release
    release_dir.mkdir(parents=True, exist_ok=True)
    jar_path = release_dir / "onesub-backend.jar"
    changed = not jar_path.is_file()
    if not jar_path.is_file():
        jar_path.write_bytes(
            get_blob(
                os.environ["ONESUB_STORAGE_ACCOUNT"],
                os.environ["ONESUB_STORAGE_CONTAINER"],
                artifact,
            )
        )
        jar_path.chmod(0o640)
        shutil.chown(release_dir, user="onesub", group="onesub")
        shutil.chown(jar_path, user="onesub", group="onesub")

    current_link = Path("/opt/onesub/current")
    if not current_link.is_symlink() or current_link.resolve() != release_dir.resolve():
        next_link = Path("/opt/onesub/current.next")
        next_link.unlink(missing_ok=True)
        next_link.symlink_to(release_dir)
        next_link.replace(current_link)
        changed = True

    vault_name = os.environ["ONESUB_KEY_VAULT"]
    database_url = get_secret(vault_name, "db-url")
    database_username = get_secret(vault_name, "db-username")
    database_password = get_secret(vault_name, "db-password")
    env_values = {
        "SPRING_PROFILES_ACTIVE": "prod",
        "ONESUB_DB_URL": database_url,
        "ONESUB_DB_USERNAME": database_username,
        "ONESUB_DB_PASSWORD": database_password,
        "ONESUB_FLYWAY_ENABLED": "false",
        "ONESUB_SERVER_PORT": "8080",
        "ONESUB_EMAIL_PROVIDER": "log",
        "JAVA_TOOL_OPTIONS": "-Xms128m -Xmx640m -XX:MaxMetaspaceSize=192m",
    }
    env_content = "".join(
        f"{name}={systemd_environment_value(value)}\n"
        for name, value in env_values.items()
    )
    env_path = Path("/etc/onesub/backend.env")
    if not env_path.exists() or env_path.read_text(encoding="utf-8") != env_content:
        changed = True
    write_secret_file(env_path, env_content, 0o600)
    subprocess.run(["systemctl", "enable", "onesub-backend.service"], check=True)
    if changed:
        subprocess.run(["systemctl", "restart", "onesub-backend.service"], check=True)


def deploy_frontend(manifest, release):
    artifact = safe_blob_name(manifest["frontend"])
    release_dir = Path("/var/www/onesub/releases") / release
    if not (release_dir / "index.html").is_file():
        extract_frontend(
            get_blob(
                os.environ["ONESUB_STORAGE_ACCOUNT"],
                os.environ["ONESUB_STORAGE_CONTAINER"],
                artifact,
            ),
            release_dir,
        )

    current_link = Path("/var/www/onesub/current")
    changed = not current_link.is_symlink() or current_link.resolve() != release_dir.resolve()
    if changed:
        next_link = Path("/var/www/onesub/current.next")
        next_link.unlink(missing_ok=True)
        next_link.symlink_to(release_dir)
        next_link.replace(current_link)

    vault_name = os.environ["ONESUB_KEY_VAULT"]
    bundle = ensure_tls_bundle(
        vault_name,
        os.environ["ONESUB_DOMAIN"],
    )
    fullchain = bundle["fullchain"]
    private_key = bundle["privateKey"]
    if "-----BEGIN CERTIFICATE-----" not in fullchain:
        raise ValueError("Key Vault TLS full chain is not PEM encoded")
    if "-----BEGIN" not in private_key or "PRIVATE KEY-----" not in private_key:
        raise ValueError("Key Vault TLS private key is not PEM encoded")

    certificate_dir = Path("/etc/ssl/onesub")
    certificate_dir.mkdir(parents=True, exist_ok=True)
    fullchain_path = certificate_dir / "fullchain.pem"
    private_key_path = certificate_dir / "privkey.pem"
    if not fullchain_path.exists() or fullchain_path.read_text(encoding="utf-8") != fullchain:
        changed = True
        write_secret_file(fullchain_path, fullchain, 0o644)
    if not private_key_path.exists() or private_key_path.read_text(encoding="utf-8") != private_key:
        changed = True
        write_secret_file(private_key_path, private_key, 0o600)

    domain = os.environ["ONESUB_DOMAIN"]
    backend_ip = os.environ["ONESUB_BACKEND_IP"]
    nginx_config = f"""server {{
    listen 80;
    server_name {domain};
    return 301 https://$host$request_uri;
}}

server {{
    listen 443 ssl http2;
    server_name {domain};
    root /var/www/onesub/current;
    index index.html;

    ssl_certificate /etc/ssl/onesub/fullchain.pem;
    ssl_certificate_key /etc/ssl/onesub/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    location /api/ {{
        proxy_pass http://{backend_ip}:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }}

    location /assets/ {{
        expires 1y;
        add_header Cache-Control "public, immutable";
        try_files $uri =404;
    }}

    location / {{
        try_files $uri $uri/ /index.html;
    }}
}}
"""
    config_path = Path("/etc/nginx/sites-available/onesub")
    if not config_path.exists() or config_path.read_text(encoding="utf-8") != nginx_config:
        config_path.write_text(nginx_config, encoding="utf-8")
        changed = True
    Path("/etc/nginx/sites-enabled/default").unlink(missing_ok=True)
    enabled_config = Path("/etc/nginx/sites-enabled/onesub")
    if not enabled_config.is_symlink() or enabled_config.resolve() != config_path.resolve():
        enabled_config.unlink(missing_ok=True)
        enabled_config.symlink_to(config_path)
        changed = True
    if changed:
        subprocess.run(["nginx", "-t"], check=True)
        subprocess.run(["systemctl", "reload", "nginx"], check=True)


def main():
    manifest_data = get_blob(
        os.environ["ONESUB_STORAGE_ACCOUNT"],
        os.environ["ONESUB_STORAGE_CONTAINER"],
        "current.json",
    )
    manifest = json.loads(manifest_data)
    release = safe_release_name(manifest["release"])
    if os.environ["ONESUB_ROLE"] == "backend":
        deploy_backend(manifest, release)
    elif os.environ["ONESUB_ROLE"] == "frontend":
        deploy_frontend(manifest, release)
    else:
        raise ValueError(f"Unsupported node role: {os.environ['ONESUB_ROLE']!r}")


if __name__ == "__main__":
    main()
