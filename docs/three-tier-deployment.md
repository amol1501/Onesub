# Azure three-tier deployment

This repository deploys OneSub as a three-tier application:

- The frontend VMSS serves the React build with Nginx and terminates HTTPS.
- The backend VMSS runs the Spring Boot executable JAR directly under Java 21
  and systemd; the JAR includes embedded Tomcat.
- A private Standard Load Balancer connects Nginx to the backend.
- PostgreSQL Flexible Server stores application data.
- CI publishes a versioned backend Maven package to Azure Artifacts. CD
  downloads that package, builds the frontend, runs Flyway, and copies release
  artifacts to private Blob Storage for the VMSS managed identities.

Docker is not used by the production deployment. The root
`docker-compose.yml` remains only for local PostgreSQL development.

VMSS instances are scheduled to run from 08:00 to 23:30 India Standard Time
and scale to zero overnight. PostgreSQL, the load balancer, public IP, storage,
and Key Vault remain allocated overnight.

## Jenkins setup

The Jenkins agent needs Homebrew OpenJDK 21 at
`/opt/homebrew/opt/openjdk@21`, Node.js/npm, Python 3, Terraform, Azure CLI,
`tar`, and `curl`. No Docker daemon or SSH client is needed for deployment.

Configure these credentials, available to the deployment job:

- Existing Azure service-principal credentials:
  `onesub-azure-client-id`, `onesub-azure-client-secret`,
  `onesub-azure-tenant-id`, `onesub-azure-subscription-id`.
- `onesub-admin-cidr` — current Jenkins egress IPv4 address with `/32`; this
  controls the PostgreSQL, Key Vault, and artifact-storage allowlists.
- `onesub-ssh-public-key` — public key installed on the VMSS instances.
- `onesub-azure-artifacts-publish-pat` — Packaging Read & Write.
- `onesub-azure-artifacts-read-pat` — Packaging Read.
- `onesub-postgres-admin-password` — a strong password of at least 16
  characters.

GoDaddy API credentials are not used. DNS records and certificate issuance
and renewal are performed manually.

Create two Jenkins Pipeline jobs from the same repository:

1. **CI:** script path `Jenkinsfile.ci`. It runs backend tests and
   `mvn clean deploy`, publishing `com.onesub:onesub-backend` with version
   `1.0.<CI-build-number>`.
2. **CD:** script path `Jenkinsfile`. Enter the exact version printed by CI
   in `ONESUB_ARTIFACT_VERSION`. CD downloads that package, runs frontend
   tests/build, plans Azure changes, and waits for manual approval before
   applying.

Use the same source revision for CI and CD so the frontend and backend
versions remain compatible.

The feed URL is
`https://pkgs.dev.azure.com/mastropayment/Onesub/_packaging/onesub/maven/v1`.
If needed, create a Maven feed named `onesub` in the `mastropayment`
organization and `Onesub` project. Grant the publish PAT Contributor and the
read PAT Reader access. The Maven server id is `onesub`; Jenkins generates a
temporary `settings.xml` and removes it after Maven runs. Never commit a PAT.

The Terraform service principal needs resource-management permissions and
`User Access Administrator` (or `Owner`) at the resource-group scope so it can
create managed-identity role assignments.

## Manual GoDaddy DNS and TLS

After Terraform creates the frontend public IP and Key Vault, the CD pipeline
pauses at **Manual DNS and TLS setup**. Use the displayed domain, IP, and Key
Vault name to complete these steps:

1. In GoDaddy, create a DNS-only apex `A` record:

   | Type | Name | Value |
   | --- | --- | --- |
   | A | `@` | The `frontend_public_ip` Terraform output |

   Remove conflicting apex `A` records. Nginx is configured for the apex
   domain only; `www` is not served by this deployment.
2. Use an ACME client in manual DNS-01 mode for the configured domain. Add the
   exact `_acme-challenge` TXT value requested by the client in GoDaddy, wait
   for DNS propagation, and complete issuance. Keep the certificate full
   chain and matching private key private.
3. Log in to Azure with an identity allowed to set secrets in the frontend
   Key Vault, then upload and validate the bundle from the repository root:

   ```sh
   az login
   az account set --subscription "<subscription-uuid>"
   bash onesub-infrastructure/scripts/upload-manual-tls-bundle.sh \
     "<frontend-key-vault-name>" "<configured-domain>" \
     "/path/to/fullchain.pem" "/path/to/privkey.pem"
   ```

   The helper validates the domain, expiry, PEM format, and certificate/key
   match, then stores a JSON value under the Key Vault secret
   `onesub-tls-bundle`. Do not paste certificate material into Jenkins
   parameters, logs, or chat.
4. Resume the Jenkins input. CD validates the Key Vault bundle before
   publishing artifacts and deploying the VMSS instances.

The frontend refresh service reads the bundle from Key Vault and reloads Nginx
when it changes. Certificates are not renewed automatically: renew well before
expiry, repeat the manual DNS-01 TXT challenge, and upload the new files with
the helper. The refresh service warns when the installed certificate is
within 30 days of expiry and fails if it is expired.

## Legacy POC transition

The existing Jenkins Terraform state tracks the single-VM POC. The POC VM,
NIC, public IP, NSG, subnet, and shutdown schedule are removed from the
configuration; the resource group and VNet remain because the three-tier
module uses them. The next Terraform plan will include deletion of those POC
resources alongside three-tier provisioning. Inspect every destroy action
before approving; this repository change does not itself apply or destroy
Azure resources.

Terraform state is local to the persistent Jenkins workspace. Keep using the
existing deployment job and workspace; do not enable workspace cleanup or run
the same state from another agent. A future production rollout should migrate
the existing state to an encrypted, access-controlled Azure Storage backend.

## Budget and operations

The VMSS schedule reduces compute time but does not stop PostgreSQL, the load
balancer, public IP, storage, or Key Vault charges. Azure budgets send alerts;
they do not enforce a hard spending ceiling. Treat `$75/month` as a target,
not a guaranteed maximum, and review the Azure estimate and approved plan.

Terraform records the PostgreSQL administrator password in state. Keep the
Jenkins workspace and state access-restricted; never copy state or plan files
into source control.

Keep the backend at one instance initially. Do not scale it out until session
state is shared or the selected affinity behavior has been load-tested.
