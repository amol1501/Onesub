#!/usr/bin/env python3

import json
import subprocess
import sys
import tempfile
from pathlib import Path


def run_openssl(arguments, input_data=None):
    result = subprocess.run(
        ["openssl", *arguments],
        input=input_data,
        capture_output=True,
        check=False,
    )
    if result.returncode:
        message = result.stderr.decode("utf-8", errors="replace").strip()
        raise ValueError(message or f"openssl {' '.join(arguments)} failed")
    return result.stdout


def validate(bundle_path, domain):
    bundle = json.loads(Path(bundle_path).read_text(encoding="utf-8"))
    if not isinstance(bundle, dict):
        raise ValueError("TLS bundle must be a JSON object")

    fullchain = bundle.get("fullchain")
    private_key = bundle.get("privateKey")
    if not isinstance(fullchain, str) or "-----BEGIN CERTIFICATE-----" not in fullchain:
        raise ValueError("TLS bundle must contain a PEM fullchain")
    if not isinstance(private_key, str) or "PRIVATE KEY-----" not in private_key:
        raise ValueError("TLS bundle must contain a PEM private key")

    with tempfile.TemporaryDirectory(prefix="onesub-tls-validation-") as directory:
        directory_path = Path(directory)
        certificate_path = directory_path / "fullchain.pem"
        private_key_path = directory_path / "privkey.pem"
        certificate_path.write_text(fullchain, encoding="utf-8")
        private_key_path.write_text(private_key, encoding="utf-8")
        certificate_path.chmod(0o600)
        private_key_path.chmod(0o600)

        run_openssl(["x509", "-in", str(certificate_path), "-noout", "-checkend", "0"])
        run_openssl(
            ["x509", "-in", str(certificate_path), "-noout", "-checkhost", domain]
        )
        certificate_public_key = run_openssl(
            ["x509", "-in", str(certificate_path), "-pubkey", "-noout"]
        )
        certificate_public_key_der = run_openssl(
            ["pkey", "-pubin", "-outform", "DER"], certificate_public_key
        )
        private_public_key_der = run_openssl(
            ["pkey", "-in", str(private_key_path), "-pubout", "-outform", "DER"]
        )
        if certificate_public_key_der != private_public_key_der:
            raise ValueError("TLS certificate and private key do not match")


def main():
    if len(sys.argv) != 3:
        raise SystemExit(
            "Usage: validate-manual-tls-bundle.py BUNDLE_JSON DOMAIN"
        )
    try:
        validate(sys.argv[1], sys.argv[2])
    except (OSError, json.JSONDecodeError, ValueError) as error:
        raise SystemExit(f"Invalid manual TLS bundle: {error}") from error


if __name__ == "__main__":
    main()
