# OneSub three-tier infrastructure

This directory contains Terraform and runtime scripts for the frontend VMSS,
backend VMSS, PostgreSQL Flexible Server, and shared Azure networking. The
backend runs as a Java 21 service; the frontend uses Nginx. Docker is not part
of the production deployment.

See [the deployment guide](../docs/three-tier-deployment.md) for Jenkins
credentials, the Azure Artifacts release flow, manual GoDaddy DNS/TLS steps,
and deployment operations.

Terraform state is currently local to the persistent Jenkins workspace. Keep
using the existing deployment job and workspace; do not enable workspace
cleanup or run the same state from another agent.

The legacy single-VM resources are being removed from Terraform configuration.
The first plan will propose deleting the tracked POC VM, its NIC and NSG
association, public IP, NSG, POC subnet, and shutdown schedule, along with its
Terraform-only DNS suffix. The shared resource group and virtual network
remain because the three-tier module uses them. Review this plan carefully
before approving it. No Azure resources are changed by editing this
repository.

The local `docker-compose.yml` at the repository root is retained only for
running PostgreSQL during development; production services do not use Docker.

The existing resource group and VNet keep their `onesub-poc-*` names so
Terraform can preserve and reuse the shared Azure foundation. Renaming those
resources is intentionally outside this cleanup.
