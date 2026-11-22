#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <vm-public-ip> <vm-public-fqdn>" >&2
  exit 2
fi

vm_ip=$1
fqdn=$2
if [[ ! $vm_ip =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; then
  echo "VM public IP must be an IPv4 address." >&2
  exit 2
fi
if [[ ! $fqdn =~ ^[a-zA-Z0-9]([a-zA-Z0-9.-]*[a-zA-Z0-9])?$ ]]; then
  echo "VM public FQDN contains invalid characters." >&2
  exit 2
fi

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ssh_host="onesubadmin@$vm_ip"
ssh_options=(-o BatchMode=yes -o StrictHostKeyChecking=accept-new)
if [[ -n "${ONESUB_SSH_KEY:-}" ]]; then
  ssh_options+=(-i "$ONESUB_SSH_KEY" -o IdentitiesOnly=yes)
fi

ssh "${ssh_options[@]}" "$ssh_host" 'sudo install -d -m 0755 /opt/onesub'
tar -czf - \
  --exclude=.git \
  --exclude=.env \
  --exclude='.env.*' \
  --exclude=node_modules \
  --exclude=target \
  --exclude=dist \
  --exclude=.terraform \
  --exclude=terraform.tfvars \
  --exclude='*.tfstate*' \
  -C "$project_root" . |
  ssh "${ssh_options[@]}" "$ssh_host" 'sudo tar -xz --no-same-owner -C /opt/onesub'

ssh "${ssh_options[@]}" "$ssh_host" "sudo bash -s -- '$fqdn'" <<'REMOTE'
set -eu
fqdn=$1
cd /opt/onesub

if [ -f .env ]; then
  db_password=$(sed -n 's/^POSTGRES_PASSWORD=//p' .env)
  if [ -z "$db_password" ]; then
    echo "Existing .env does not contain POSTGRES_PASSWORD; refusing to replace it." >&2
    exit 1
  fi
else
  db_password=$(openssl rand -hex 32)
fi

umask 077
printf 'ONESUB_FQDN=%s\nPOSTGRES_DB=onesub\nPOSTGRES_USER=onesub\nPOSTGRES_PASSWORD=%s\n' \
  "$fqdn" "$db_password" > .env
chmod 600 .env
docker compose --env-file .env -f docker-compose.azure.yml up -d --build
REMOTE
