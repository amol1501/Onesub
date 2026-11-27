output "public_ip_address" {
  description = "Public IPv4 address of the POC VM."
  value       = azurerm_public_ip.onesub.ip_address
}

output "public_fqdn" {
  description = "DNS name used by Caddy to obtain and renew an HTTPS certificate."
  value       = azurerm_public_ip.onesub.fqdn
}

output "resource_group_name" {
  description = "Resource group containing the POC VM."
  value       = azurerm_resource_group.onesub.name
}

output "virtual_machine_name" {
  description = "POC VM name used by Azure CLI."
  value       = azurerm_linux_virtual_machine.onesub.name
}

output "ssh_command" {
  description = "SSH command for connecting from the allowed admin IP."
  value       = "ssh onesubadmin@${azurerm_public_ip.onesub.ip_address}"
}

output "frontend_public_ip" {
  description = "Static frontend load-balancer IP to use for the GoDaddy apex A record."
  value       = var.enable_three_tier ? module.three_tier[0].frontend_public_ip : null
}

output "backend_internal_load_balancer_ip" {
  description = "Private address used by the frontend to proxy API requests to the backend."
  value       = var.enable_three_tier ? module.three_tier[0].backend_internal_load_balancer_ip : null
}

output "postgres_fqdn" {
  description = "Hostname of the new PostgreSQL Flexible Server."
  value       = var.enable_three_tier ? module.three_tier[0].postgres_fqdn : null
}

output "postgres_database_name" {
  description = "Database created for the new OneSub deployment."
  value       = var.enable_three_tier ? module.three_tier[0].postgres_database_name : null
}

output "artifact_storage_account_name" {
  description = "Private Blob Storage account used for versioned application artifacts."
  value       = var.enable_three_tier ? module.three_tier[0].artifact_storage_account_name : null
}

output "frontend_key_vault_name" {
  description = "Key Vault for frontend DNS challenge and TLS secrets."
  value       = var.enable_three_tier ? module.three_tier[0].frontend_key_vault_name : null
}

output "backend_key_vault_name" {
  description = "Key Vault for backend database credentials."
  value       = var.enable_three_tier ? module.three_tier[0].backend_key_vault_name : null
}

output "domain_name" {
  description = "Apex domain configured for the frontend."
  value       = var.enable_three_tier ? module.three_tier[0].domain_name : null
}

output "postgres_admin_username" {
  description = "PostgreSQL administrator username used by the migration stage."
  value       = var.enable_three_tier ? module.three_tier[0].postgres_admin_username : null
}

output "frontend_vmss_name" {
  description = "Name of the frontend virtual machine scale set."
  value       = var.enable_three_tier ? module.three_tier[0].frontend_vmss_name : null
}

output "backend_vmss_name" {
  description = "Name of the backend virtual machine scale set."
  value       = var.enable_three_tier ? module.three_tier[0].backend_vmss_name : null
}
