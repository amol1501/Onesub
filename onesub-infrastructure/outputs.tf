output "resource_group_name" {
  description = "Resource group containing the OneSub three-tier deployment."
  value       = azurerm_resource_group.onesub.name
}

output "frontend_public_ip" {
  description = "Static frontend load-balancer IP to use for the GoDaddy apex A record."
  value       = module.three_tier.frontend_public_ip
}

output "backend_internal_load_balancer_ip" {
  description = "Private address used by the frontend to proxy API requests to the backend."
  value       = module.three_tier.backend_internal_load_balancer_ip
}

output "postgres_fqdn" {
  description = "Hostname of the PostgreSQL Flexible Server."
  value       = module.three_tier.postgres_fqdn
}

output "postgres_database_name" {
  description = "Database created for OneSub."
  value       = module.three_tier.postgres_database_name
}

output "artifact_storage_account_name" {
  description = "Private Blob Storage account used for versioned application artifacts."
  value       = module.three_tier.artifact_storage_account_name
}

output "frontend_key_vault_name" {
  description = "Key Vault for the manually managed frontend TLS bundle."
  value       = module.three_tier.frontend_key_vault_name
}

output "backend_key_vault_name" {
  description = "Key Vault for backend database credentials."
  value       = module.three_tier.backend_key_vault_name
}

output "domain_name" {
  description = "Domain configured for the frontend."
  value       = module.three_tier.domain_name
}

output "postgres_admin_username" {
  description = "PostgreSQL administrator username used by the migration stage."
  value       = module.three_tier.postgres_admin_username
}

output "frontend_vmss_name" {
  description = "Name of the frontend virtual machine scale set."
  value       = module.three_tier.frontend_vmss_name
}

output "backend_vmss_name" {
  description = "Name of the backend virtual machine scale set."
  value       = module.three_tier.backend_vmss_name
}
