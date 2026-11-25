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

output "artifactory_vm_name" {
  description = "Name of the dedicated Artifactory OSS VM."
  value       = azurerm_linux_virtual_machine.artifactory.name
}

output "artifactory_public_ip" {
  description = "Public IPv4 address used only for SSH access to the Artifactory VM."
  value       = azurerm_public_ip.artifactory.ip_address
}

output "artifactory_private_ip" {
  description = "Private VNet address used by OneSub workloads to access Artifactory."
  value       = azurerm_network_interface.artifactory.private_ip_address
}

output "artifactory_ssh_tunnel_command" {
  description = "SSH tunnel for opening the private Artifactory UI on localhost:8082."
  value       = "ssh -N -L 8082:${azurerm_network_interface.artifactory.private_ip_address}:8082 onesubadmin@${azurerm_public_ip.artifactory.ip_address}"
}
