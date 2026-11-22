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
