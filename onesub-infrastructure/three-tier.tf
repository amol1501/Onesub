module "three_tier" {
  count  = var.enable_three_tier ? 1 : 0
  source = "./modules/three-tier"

  resource_group_name        = azurerm_resource_group.onesub.name
  location                   = azurerm_resource_group.onesub.location
  virtual_network_name       = azurerm_virtual_network.onesub.name
  tags                       = azurerm_resource_group.onesub.tags
  name_prefix                = var.name_prefix
  admin_cidr                 = var.admin_cidr
  ssh_public_key             = var.ssh_public_key
  ssh_public_key_path        = var.ssh_public_key_path
  domain_name                = var.domain_name
  frontend_vm_size           = var.frontend_vm_size
  backend_vm_size            = var.backend_vm_size
  postgres_admin_username    = var.postgres_admin_username
  postgres_admin_password    = var.postgres_admin_password
  postgres_sku_name          = var.postgres_sku_name
  postgres_storage_mb        = var.postgres_storage_mb
  business_hours_start_hour  = var.business_hours_start_hour
  business_hours_stop_hour   = var.business_hours_stop_hour
  business_hours_stop_minute = var.business_hours_stop_minute
}
