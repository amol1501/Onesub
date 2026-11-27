#!/usr/bin/env python3

import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


SETTINGS_NAMESPACE = "http://maven.apache.org/SETTINGS/1.0.0"
XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance"


def main():
    if len(sys.argv) != 2:
        raise SystemExit("Usage: create-azure-artifacts-settings.py OUTPUT_PATH")

    token = os.environ.get("AZURE_ARTIFACTS_PAT")
    if not token:
        raise SystemExit("AZURE_ARTIFACTS_PAT must be set")

    ET.register_namespace("", SETTINGS_NAMESPACE)
    ET.register_namespace("xsi", XSI_NAMESPACE)
    settings = ET.Element(
        f"{{{SETTINGS_NAMESPACE}}}settings",
        {
            f"{{{XSI_NAMESPACE}}}schemaLocation": (
                f"{SETTINGS_NAMESPACE} "
                "https://maven.apache.org/xsd/settings-1.0.0.xsd"
            )
        },
    )
    servers = ET.SubElement(settings, f"{{{SETTINGS_NAMESPACE}}}servers")
    server = ET.SubElement(servers, f"{{{SETTINGS_NAMESPACE}}}server")
    ET.SubElement(server, f"{{{SETTINGS_NAMESPACE}}}id").text = "onesub"
    ET.SubElement(server, f"{{{SETTINGS_NAMESPACE}}}username").text = "mastropayment"
    ET.SubElement(server, f"{{{SETTINGS_NAMESPACE}}}password").text = token

    output_path = Path(sys.argv[1])
    output_path.write_bytes(ET.tostring(settings, encoding="utf-8", xml_declaration=True))
    output_path.chmod(0o600)


if __name__ == "__main__":
    main()
