output "frontend_public_ip" {
  value = azurerm_public_ip.three_tier.ip_address
}

output "backend_internal_load_balancer_ip" {
  value = azurerm_lb.three_tier.frontend_ip_configuration[1].private_ip_address
}

output "postgres_fqdn" {
  value = azurerm_postgresql_flexible_server.onesub.fqdn
}

output "postgres_database_name" {
  value = azurerm_postgresql_flexible_server_database.onesub.name
}

output "artifact_storage_account_name" {
  value = azurerm_storage_account.artifacts.name
}

output "frontend_key_vault_name" {
  value = azurerm_key_vault.frontend.name
}

output "backend_key_vault_name" {
  value = azurerm_key_vault.backend.name
}

output "domain_name" {
  value = var.domain_name
}

output "postgres_admin_username" {
  value = var.postgres_admin_username
}

output "frontend_vmss_name" {
  value = azurerm_linux_virtual_machine_scale_set.frontend.name
}

output "backend_vmss_name" {
  value = azurerm_linux_virtual_machine_scale_set.backend.name
}
