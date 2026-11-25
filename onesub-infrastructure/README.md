# OneSub Azure POC

This is a practice deployment, not a production availability design. Terraform creates one small Ubuntu VM in Central India. Docker Compose runs PostgreSQL, the Spring Boot API, the React site, and Caddy for automatic HTTPS. PostgreSQL is private to the Compose network, and Caddy proxies the site and `/api` through one origin so session-cookie and CSRF behavior is preserved.

The VM uses the `Standard_B1ms` size (2 GiB RAM), a 30 GiB Standard HDD OS disk, and a static public IP. Auto-shutdown deallocates the VM daily at 23:30 India Standard Time. Deallocation stops VM compute charges but does not stop disk or public-IP charges. The $30/month target is not guaranteed: actual cost depends on region, runtime, network use, and current Azure prices. Configure a separate Azure Cost Management budget alert; an alert is not a spending cap.

## Prerequisites

- An Azure subscription you explicitly choose, with permission to create resource groups, networking, public IPs, and VMs.
- Azure CLI installed. For manual Terraform use, sign in with `az login`; the Jenkins pipeline authenticates with its configured service principal.
- Terraform 1.5 or later.
- An SSH key pair. Only the public key is uploaded; keep the private key local.
- Azure CLI, Terraform 1.5 or later, SSH, `tar`, and `curl` on the Jenkins agent. A Docker client is not required on your workstation; the VM builds the application images.

## Jenkins pipeline

The repository-root `Jenkinsfile` supports a manually started Pipeline job. It validates Terraform, shows a plan, waits for approval before applying, deploys over SSH, and checks the HTTPS CSRF endpoint.

Install the Jenkins **Pipeline**, **Credentials Binding**, and **SSH Agent** plugins. Create these Jenkins credentials:

| Credential ID | Jenkins credential type | Value |
| --- | --- | --- |
| `onesub-azure-client-id` | Secret text | Azure service principal client ID |
| `onesub-azure-client-secret` | Secret text | Azure service principal client secret |
| `onesub-azure-tenant-id` | Secret text | Azure tenant ID |
| `onesub-azure-subscription-id` | Secret text | Target subscription UUID |
| `onesub-admin-cidr` | Secret text | Jenkins agent's public IPv4 address with `/32` |
| `onesub-ssh-public-key` | Secret text | Public key corresponding to the VM SSH credential |
| `onesub-vm-ssh` | SSH Username with private key | Private key for user `onesubadmin` |

Use a dedicated practice subscription. Since Terraform creates the resource group, the service principal needs subscription-level permission to create resource groups and their resources (typically `Contributor`); do not grant this to a production subscription. For a shared subscription, pre-create the resource group and adjust Terraform to use it before assigning narrower permissions. The VM's SSH rule is limited to the configured `/32`. On a local Mac agent, make sure Jenkins can find Azure CLI, Terraform, SSH, `tar`, and `curl` in its `PATH`. Keep the job's workspace and Terraform state; do not enable workspace cleanup between builds.

In Jenkins, create **New Item → Pipeline**, then select **Pipeline script from SCM** with the Git repository, branch `main`, and script path `Jenkinsfile`. Run it manually and review the plan before selecting **Apply**. The first run may take several minutes while the VM installs Docker and Caddy obtains its certificate.

The pipeline uses local Terraform state in the Jenkins workspace, suitable only for this single-agent POC. Do not run Terraform for the same resources from another workspace. Configure remote state before using multiple agents or treating this as a durable deployment.

## Configure and review

From the OneSub project root, enter this directory:

```sh
cd onesub-infrastructure
```

1. Copy `terraform.tfvars.example` to `terraform.tfvars`.
2. Set `subscription_id` to the intended subscription UUID. Do not rely on the Azure CLI's currently selected subscription.
3. Set `admin_cidr` to your current public IPv4 address with `/32` (for example, `198.51.100.25/32`), and set the public-key path if it differs from `~/.ssh/id_rsa.pub`.
4. Log in and select the same subscription:

   ```sh
   az login
   az account set --subscription "<subscription-uuid>"
   ```

5. Initialize and inspect the plan; **do not apply until you have reviewed the subscription, region, VM size, network rules, and estimated costs**:

   ```sh
   terraform init
   terraform fmt -check
   terraform validate
   terraform plan
   ```

Terraform state is local and ignored by Git. Keep it private and do not delete it while the resources exist; it is required to manage or destroy them.

## Create and deploy

After confirming the plan and the current Azure estimate, create the resources:

```sh
terraform apply
```

Wait for cloud-init to finish, then deploy the current checkout:

```sh
bash ./deploy.sh "$(terraform output -raw public_ip_address)" "$(terraform output -raw public_fqdn)"
```

The deploy script transfers source over SSH, creates a random database password on the VM, stores it in `/opt/onesub/.env` with mode `0600`, and builds/starts the stack. It does not send application or database credentials to GitHub. Caddy obtains a certificate automatically once DNS and ports 80/443 are reachable; initial HTTPS readiness can take a few minutes.

Open the `https://` URL shown by:

```sh
terraform output -raw public_fqdn
```

The `/` path serves the React app; `/api/v1` reaches Spring Boot. Register a POC account through the app to verify authentication.

## Practice and cleanup

- The VM is configured to shut down and deallocate at 23:30 IST. Start it when needed:

  ```sh
  az vm start --resource-group onesub-poc-rg --name onesub-poc-vm
  ```

- Check Compose health and logs over SSH:

  ```sh
  ssh onesubadmin@"$(terraform output -raw public_ip_address)" \
    'cd /opt/onesub && sudo docker compose --env-file .env -f docker-compose.azure.yml ps'
  ```

- SSH is allowed only from the `/32` admin IP used at creation. If your public IP changes, update `admin_cidr`, review the plan, and apply the network-rule change.
- Create and download a database backup before removing the POC:

  ```sh
  ssh onesubadmin@"$(terraform output -raw public_ip_address)" \
    'cd /opt/onesub && sudo docker compose --env-file .env -f docker-compose.azure.yml exec -T db pg_dump -U onesub onesub' \
    > onesub-poc.sql
  ```

- `terraform destroy` removes the VM, its disk, and the database volume. Back up any data you need first. The static public IP and disk can incur charges until destroyed.
- Use Azure Cost Management to create a $30 monthly budget alert for this resource group. Alerts notify; they do not automatically stop spending.

This POC has no high availability, managed database, automated database backup, or production disaster recovery. Do not store sensitive or real payment information in it.
