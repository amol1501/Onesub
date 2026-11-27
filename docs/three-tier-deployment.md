# Azure three-tier deployment

The existing single-VM POC remains available. The Jenkins parameter defaults to
`poc`; select `three-tier` to create the new frontend VMSS, backend VMSS, and
PostgreSQL Flexible Server using the existing Terraform state.

## Architecture

- The frontend VMSS serves the React build with Nginx and terminates HTTPS.
- The backend VMSS runs the Spring Boot executable JAR directly under Java 21
  and systemd; the JAR includes embedded Tomcat.
- A private Standard Load Balancer endpoint connects Nginx to the backend.
  The backend rule uses source-IP affinity for the current session-based auth.
- PostgreSQL Flexible Server is a fresh database. Its firewall allows the
  Jenkins egress `/32` and the backend load-balancer public IP only.
- The Jenkins CI job publishes the backend JAR as a versioned Maven package to
  the private Azure Artifacts feed. The CD job downloads the selected version
  and copies it to private Blob Storage because the VMSS managed identities
  read runtime artifacts from Blob. The frontend bundle and Lego binary remain
  in Blob Storage.
- Flyway runs from Jenkins after infrastructure creation and before publishing
  the release pointer. Production VMSS instances have startup migrations
  disabled; the existing POC keeps its current startup-migration behavior.

The VMSS instances are scheduled to run daily from 08:00 to 23:30 India
Standard Time, and scale to zero overnight. The website is unavailable during
that time. PostgreSQL, the load balancer, and the static public IP remain
allocated overnight.

## Jenkins setup

The Jenkins agent must have Homebrew OpenJDK 21 at
`/opt/homebrew/opt/openjdk@21`, Node.js/npm, Python 3, Terraform, and Azure CLI
available. The JDK runs Maven tests, publishing and Flyway; Node.js builds the
frontend.

Keep the existing Azure and SSH credentials. Add these Jenkins **Secret text**
credentials:

- `onesub-azure-artifacts-publish-pat` — Azure Artifacts Packaging Read & Write.
- `onesub-azure-artifacts-read-pat` — Azure Artifacts Packaging Read.
- `onesub-postgres-admin-password` — use a strong password of at least 16
  characters.
- `onesub-godaddy-api-key`
- `onesub-godaddy-api-secret`

Create two Jenkins Pipeline jobs from the same repository:

1. **CI:** set the script path to `Jenkinsfile.ci` and configure SCM polling or
   a webhook. It runs `mvn clean deploy`, including backend tests, and publishes
   `com.onesub:onesub-backend` to Azure Artifacts. The CI console prints the
   immutable version as `1.0.<CI-build-number>`.
2. **CD:** keep the existing deployment job on `Jenkinsfile`. Select
   `three-tier` and enter the exact artifact version printed by CI in
   `ONESUB_ARTIFACT_VERSION`. CD downloads that JAR before planning Azure
   changes, then requires the existing manual approval before applying. It
   runs Flyway and copies the selected JAR to Blob for VMSS deployment.

Run CD from the same source revision used for the CI build so the frontend and
the selected backend JAR stay compatible.

The Azure Artifacts feed is
`https://pkgs.dev.azure.com/mastropayment/Onesub/_packaging/onesub/maven/v1`.
If it does not exist yet, create a Maven feed named `onesub` in the
`mastropayment` organization and `Onesub` project. Grant the publish PAT owner
feed Contributor access and the read PAT owner feed Reader access. The POM uses
the Maven server id `onesub`. Jenkins creates a temporary `settings.xml` from
the PAT credentials and removes it after each Maven operation; no PAT belongs
in the repository. For local Maven use, add a server with id `onesub`, username
`mastropayment`, and your PAT as the password in
`${user.home}/.m2/settings.xml`. Keep that file private and do not commit it.

The GoDaddy API credentials are stored in the frontend Key Vault for
certificate renewal. Use an API key intended for DNS automation; do not use
the GoDaddy account password.

The Terraform service principal must be able to create Azure role assignments
for managed identities. It needs resource-management permissions plus
`User Access Administrator` (or `Owner`) at the resource-group scope. The
`onesub-admin-cidr` Jenkins credential must contain the current Jenkins
egress IPv4 address as a `/32`; this controls the PostgreSQL, Key Vault, and
artifact-storage allowlists.

Review the CD plan and use the manual approval gate. Run deployment during the
scheduled hours; outside those hours the VMSS instances are scaled to zero and
the final instance deployment check cannot complete.

## GoDaddy DNS

After the first successful infrastructure apply, read the
`frontend_public_ip` Terraform output and create an apex `A` record in GoDaddy:

| Type | Name | Value |
| --- | --- | --- |
| A | `@` | The `frontend_public_ip` value |

Use DNS-only records (no proxy) and remove any conflicting apex `A` records.
The deployment uses a Let's Encrypt DNS-01 challenge, so certificate issuance
does not depend on DNS propagation. The frontend VMSS stores the certificate in
Key Vault and renews it before expiry.

## Budget and state cautions

The VMSS schedule reduces compute time, but it does not stop PostgreSQL or
remove load-balancer, IP, storage, and Key Vault charges. Azure budgets send
alerts; they do not enforce a hard spending ceiling. Treat `$75/month` as a
target, not a guaranteed maximum, and review the Azure estimate and approved
Terraform plan before applying.

The PostgreSQL administrator password is a sensitive Terraform input, but
Terraform still records it in state. The existing Jenkins workspace state must
remain access-restricted; do not copy it into source control or expose plan
files. A future production rollout should migrate the existing state (not
create a second state) to an encrypted, access-controlled Azure Storage backend.

The backend is intentionally kept at one instance initially. Do not increase
its scale until session state is shared or the selected affinity behavior has
been load-tested.
