data "azurerm_client_config" "current" {}

resource "random_id" "three_tier_suffix" {
  byte_length = 3
}

locals {
  schedule_days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
  storage_account_name = lower(
    replace("${var.name_prefix}${random_id.three_tier_suffix.hex}artifacts", "-", "")
  )
  frontend_key_vault_name = "onesub-${random_id.three_tier_suffix.hex}-edge"
  backend_key_vault_name  = "onesub-${random_id.three_tier_suffix.hex}-app"
}

resource "azurerm_subnet" "frontend" {
  name                 = "${var.name_prefix}-frontend-subnet"
  resource_group_name  = var.resource_group_name
  virtual_network_name = var.virtual_network_name
  address_prefixes     = ["10.40.2.0/24"]
  service_endpoints    = ["Microsoft.KeyVault", "Microsoft.Storage"]
}

resource "azurerm_subnet" "backend" {
  name                 = "${var.name_prefix}-backend-subnet"
  resource_group_name  = var.resource_group_name
  virtual_network_name = var.virtual_network_name
  address_prefixes     = ["10.40.3.0/24"]
  service_endpoints    = ["Microsoft.KeyVault", "Microsoft.Storage"]
}

resource "azurerm_network_security_group" "frontend" {
  name                = "${var.name_prefix}-frontend-nsg"
  location            = var.location
  resource_group_name = var.resource_group_name

  security_rule {
    name                       = "http-from-internet"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "80"
    source_address_prefix      = "Internet"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "https-from-internet"
    priority                   = 110
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "Internet"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "https-health-probe"
    priority                   = 120
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "AzureLoadBalancer"
    destination_address_prefix = "*"
  }
}

resource "azurerm_network_security_group" "backend" {
  name                = "${var.name_prefix}-backend-nsg"
  location            = var.location
  resource_group_name = var.resource_group_name

  security_rule {
    name                       = "api-from-frontend"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "8080"
    source_address_prefix      = "10.40.2.0/24"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "api-health-probe"
    priority                   = 110
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "8080"
    source_address_prefix      = "AzureLoadBalancer"
    destination_address_prefix = "*"
  }
}

resource "azurerm_subnet_network_security_group_association" "frontend" {
  subnet_id                 = azurerm_subnet.frontend.id
  network_security_group_id = azurerm_network_security_group.frontend.id
}

resource "azurerm_subnet_network_security_group_association" "backend" {
  subnet_id                 = azurerm_subnet.backend.id
  network_security_group_id = azurerm_network_security_group.backend.id
}

resource "azurerm_public_ip" "three_tier" {
  name                = "${var.name_prefix}-frontend-lb-ip"
  location            = var.location
  resource_group_name = var.resource_group_name
  allocation_method   = "Static"
  sku                 = "Standard"
  tags                = var.tags
}

resource "azurerm_lb" "three_tier" {
  name                = "${var.name_prefix}-lb"
  location            = var.location
  resource_group_name = var.resource_group_name
  sku                 = "Standard"

  frontend_ip_configuration {
    name                 = "public"
    public_ip_address_id = azurerm_public_ip.three_tier.id
  }

  frontend_ip_configuration {
    name                          = "backend-private"
    subnet_id                     = azurerm_subnet.backend.id
    private_ip_address            = "10.40.3.4"
    private_ip_address_allocation = "Static"
  }

  tags = var.tags
}

resource "azurerm_lb_backend_address_pool" "frontend" {
  name            = "${var.name_prefix}-frontend-pool"
  loadbalancer_id = azurerm_lb.three_tier.id
}

resource "azurerm_lb_backend_address_pool" "backend" {
  name            = "${var.name_prefix}-backend-pool"
  loadbalancer_id = azurerm_lb.three_tier.id
}

resource "azurerm_lb_probe" "frontend" {
  name            = "${var.name_prefix}-frontend-https"
  loadbalancer_id = azurerm_lb.three_tier.id
  protocol        = "Tcp"
  port            = 443
}

resource "azurerm_lb_probe" "backend" {
  name            = "${var.name_prefix}-backend-api"
  loadbalancer_id = azurerm_lb.three_tier.id
  protocol        = "Tcp"
  port            = 8080
}

resource "azurerm_lb_rule" "frontend_https" {
  name                           = "${var.name_prefix}-https"
  loadbalancer_id                = azurerm_lb.three_tier.id
  protocol                       = "Tcp"
  frontend_port                  = 443
  backend_port                   = 443
  frontend_ip_configuration_name = "public"
  backend_address_pool_ids       = [azurerm_lb_backend_address_pool.frontend.id]
  probe_id                       = azurerm_lb_probe.frontend.id
  load_distribution              = "SourceIP"
  disable_outbound_snat          = true
}

resource "azurerm_lb_rule" "frontend_http" {
  name                           = "${var.name_prefix}-http"
  loadbalancer_id                = azurerm_lb.three_tier.id
  protocol                       = "Tcp"
  frontend_port                  = 80
  backend_port                   = 80
  frontend_ip_configuration_name = "public"
  backend_address_pool_ids       = [azurerm_lb_backend_address_pool.frontend.id]
  probe_id                       = azurerm_lb_probe.frontend.id
  load_distribution              = "SourceIP"
  disable_outbound_snat          = true
}

resource "azurerm_lb_rule" "backend_api" {
  name                           = "${var.name_prefix}-backend-api"
  loadbalancer_id                = azurerm_lb.three_tier.id
  protocol                       = "Tcp"
  frontend_port                  = 8080
  backend_port                   = 8080
  frontend_ip_configuration_name = "backend-private"
  backend_address_pool_ids       = [azurerm_lb_backend_address_pool.backend.id]
  probe_id                       = azurerm_lb_probe.backend.id
  load_distribution              = "SourceIP"
  disable_outbound_snat          = true
}

resource "azurerm_lb_outbound_rule" "frontend" {
  name                     = "${var.name_prefix}-frontend-egress"
  loadbalancer_id          = azurerm_lb.three_tier.id
  protocol                 = "All"
  backend_address_pool_id  = azurerm_lb_backend_address_pool.frontend.id
  allocated_outbound_ports = 1024
  enable_tcp_reset         = true
  idle_timeout_in_minutes  = 15

  frontend_ip_configuration {
    name = "public"
  }
}

resource "azurerm_lb_outbound_rule" "backend" {
  name                     = "${var.name_prefix}-backend-egress"
  loadbalancer_id          = azurerm_lb.three_tier.id
  protocol                 = "All"
  backend_address_pool_id  = azurerm_lb_backend_address_pool.backend.id
  allocated_outbound_ports = 1024
  enable_tcp_reset         = true
  idle_timeout_in_minutes  = 15

  frontend_ip_configuration {
    name = "public"
  }
}

resource "azurerm_storage_account" "artifacts" {
  name                            = local.storage_account_name
  resource_group_name             = var.resource_group_name
  location                        = var.location
  account_tier                    = "Standard"
  account_replication_type        = "LRS"
  min_tls_version                 = "TLS1_2"
  https_traffic_only_enabled      = true
  allow_nested_items_to_be_public = false
  public_network_access_enabled   = true

  network_rules {
    default_action             = "Deny"
    bypass                     = ["AzureServices"]
    ip_rules                   = [var.admin_cidr]
    virtual_network_subnet_ids = [azurerm_subnet.frontend.id, azurerm_subnet.backend.id]
  }

  tags = var.tags
}

resource "azurerm_storage_container" "artifacts" {
  name                  = "onesub-artifacts"
  storage_account_name  = azurerm_storage_account.artifacts.name
  container_access_type = "private"
}

resource "azurerm_key_vault" "frontend" {
  name                          = local.frontend_key_vault_name
  location                      = var.location
  resource_group_name           = var.resource_group_name
  tenant_id                     = data.azurerm_client_config.current.tenant_id
  sku_name                      = "standard"
  enable_rbac_authorization     = true
  soft_delete_retention_days    = 7
  purge_protection_enabled      = false
  public_network_access_enabled = true

  network_acls {
    bypass                     = "AzureServices"
    default_action             = "Deny"
    ip_rules                   = [var.admin_cidr]
    virtual_network_subnet_ids = [azurerm_subnet.frontend.id]
  }

  tags = var.tags
}

resource "azurerm_key_vault" "backend" {
  name                          = local.backend_key_vault_name
  location                      = var.location
  resource_group_name           = var.resource_group_name
  tenant_id                     = data.azurerm_client_config.current.tenant_id
  sku_name                      = "standard"
  enable_rbac_authorization     = true
  soft_delete_retention_days    = 7
  purge_protection_enabled      = false
  public_network_access_enabled = true

  network_acls {
    bypass                     = "AzureServices"
    default_action             = "Deny"
    ip_rules                   = [var.admin_cidr]
    virtual_network_subnet_ids = [azurerm_subnet.backend.id]
  }

  tags = var.tags
}

resource "azurerm_role_assignment" "jenkins_artifacts" {
  scope                = azurerm_storage_account.artifacts.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = data.azurerm_client_config.current.object_id
}

resource "azurerm_role_assignment" "frontend_artifacts" {
  scope                = azurerm_storage_account.artifacts.id
  role_definition_name = "Storage Blob Data Reader"
  principal_id         = azurerm_linux_virtual_machine_scale_set.frontend.identity[0].principal_id
}

resource "azurerm_role_assignment" "backend_artifacts" {
  scope                = azurerm_storage_account.artifacts.id
  role_definition_name = "Storage Blob Data Reader"
  principal_id         = azurerm_linux_virtual_machine_scale_set.backend.identity[0].principal_id
}

resource "azurerm_role_assignment" "jenkins_frontend_secrets" {
  scope                = azurerm_key_vault.frontend.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current.object_id
}

resource "azurerm_role_assignment" "frontend_secrets" {
  scope                = azurerm_key_vault.frontend.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = azurerm_linux_virtual_machine_scale_set.frontend.identity[0].principal_id
}

resource "azurerm_role_assignment" "jenkins_backend_secrets" {
  scope                = azurerm_key_vault.backend.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current.object_id
}

resource "azurerm_role_assignment" "backend_secrets" {
  scope                = azurerm_key_vault.backend.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_linux_virtual_machine_scale_set.backend.identity[0].principal_id
}

resource "azurerm_postgresql_flexible_server" "onesub" {
  name                          = "onesub-db-${random_id.three_tier_suffix.hex}"
  resource_group_name           = var.resource_group_name
  location                      = var.location
  version                       = "16"
  administrator_login           = var.postgres_admin_username
  administrator_password        = var.postgres_admin_password
  sku_name                      = var.postgres_sku_name
  storage_mb                    = var.postgres_storage_mb
  backup_retention_days         = 7
  geo_redundant_backup_enabled  = false
  auto_grow_enabled             = false
  public_network_access_enabled = true
  tags                          = var.tags
}

resource "azurerm_postgresql_flexible_server_database" "onesub" {
  name      = "onesub"
  server_id = azurerm_postgresql_flexible_server.onesub.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

resource "azurerm_postgresql_flexible_server_firewall_rule" "jenkins" {
  name             = "jenkins-deployment-host"
  server_id        = azurerm_postgresql_flexible_server.onesub.id
  start_ip_address = cidrhost(var.admin_cidr, 0)
  end_ip_address   = cidrhost(var.admin_cidr, 0)
}

resource "azurerm_postgresql_flexible_server_firewall_rule" "backend_egress" {
  name             = "backend-vmss-egress"
  server_id        = azurerm_postgresql_flexible_server.onesub.id
  start_ip_address = azurerm_public_ip.three_tier.ip_address
  end_ip_address   = azurerm_public_ip.three_tier.ip_address
}

resource "azurerm_linux_virtual_machine_scale_set" "frontend" {
  name                            = "${var.name_prefix}-frontend-vmss"
  location                        = var.location
  resource_group_name             = var.resource_group_name
  sku                             = var.frontend_vm_size
  instances                       = 0
  admin_username                  = "onesubadmin"
  disable_password_authentication = true
  upgrade_mode                    = "Manual"
  overprovision                   = false
  custom_data = base64encode(templatefile("${path.root}/frontend-cloud-init.yaml.tftpl", {
    domain_name          = var.domain_name
    storage_account_name = azurerm_storage_account.artifacts.name
    storage_container    = azurerm_storage_container.artifacts.name
    frontend_vault_name  = azurerm_key_vault.frontend.name
    backend_private_ip   = "10.40.3.4"
    refresh_script_b64   = base64encode(file("${path.root}/scripts/refresh-node.py"))
  }))

  identity {
    type = "SystemAssigned"
  }

  admin_ssh_key {
    username   = "onesubadmin"
    public_key = var.ssh_public_key != null ? var.ssh_public_key : file(pathexpand(var.ssh_public_key_path))
  }

  network_interface {
    name    = "${var.name_prefix}-frontend-nic"
    primary = true

    ip_configuration {
      name                                   = "internal"
      primary                                = true
      subnet_id                              = azurerm_subnet.frontend.id
      load_balancer_backend_address_pool_ids = [azurerm_lb_backend_address_pool.frontend.id]
    }
  }

  os_disk {
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"
    disk_size_gb         = 30
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "ubuntu-24_04-lts"
    sku       = "server"
    version   = "latest"
  }

  lifecycle {
    ignore_changes = [instances]
  }

  tags = var.tags
}

resource "azurerm_linux_virtual_machine_scale_set" "backend" {
  name                            = "${var.name_prefix}-backend-vmss"
  location                        = var.location
  resource_group_name             = var.resource_group_name
  sku                             = var.backend_vm_size
  instances                       = 0
  admin_username                  = "onesubadmin"
  disable_password_authentication = true
  upgrade_mode                    = "Manual"
  overprovision                   = false
  custom_data = base64encode(templatefile("${path.root}/backend-cloud-init.yaml.tftpl", {
    storage_account_name = azurerm_storage_account.artifacts.name
    storage_container    = azurerm_storage_container.artifacts.name
    backend_vault_name   = azurerm_key_vault.backend.name
    postgres_fqdn        = azurerm_postgresql_flexible_server.onesub.fqdn
    refresh_script_b64   = base64encode(file("${path.root}/scripts/refresh-node.py"))
  }))

  identity {
    type = "SystemAssigned"
  }

  admin_ssh_key {
    username   = "onesubadmin"
    public_key = var.ssh_public_key != null ? var.ssh_public_key : file(pathexpand(var.ssh_public_key_path))
  }

  network_interface {
    name    = "${var.name_prefix}-backend-nic"
    primary = true

    ip_configuration {
      name                                   = "internal"
      primary                                = true
      subnet_id                              = azurerm_subnet.backend.id
      load_balancer_backend_address_pool_ids = [azurerm_lb_backend_address_pool.backend.id]
    }
  }

  os_disk {
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"
    disk_size_gb         = 30
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "ubuntu-24_04-lts"
    sku       = "server"
    version   = "latest"
  }

  lifecycle {
    ignore_changes = [instances]
  }

  tags = var.tags
}

resource "azurerm_monitor_autoscale_setting" "frontend" {
  name                = "${var.name_prefix}-frontend-hours"
  location            = var.location
  resource_group_name = var.resource_group_name
  target_resource_id  = azurerm_linux_virtual_machine_scale_set.frontend.id

  profile {
    name = "business-hours"
    capacity {
      default = 1
      minimum = 1
      maximum = 1
    }
    recurrence {
      timezone = "India Standard Time"
      days     = local.schedule_days
      hours    = [var.business_hours_start_hour]
      minutes  = [0]
    }
  }

  profile {
    name = "nightly-shutdown"
    capacity {
      default = 0
      minimum = 0
      maximum = 0
    }
    recurrence {
      timezone = "India Standard Time"
      days     = local.schedule_days
      hours    = [var.business_hours_stop_hour]
      minutes  = [var.business_hours_stop_minute]
    }
  }
}

resource "azurerm_monitor_autoscale_setting" "backend" {
  name                = "${var.name_prefix}-backend-hours"
  location            = var.location
  resource_group_name = var.resource_group_name
  target_resource_id  = azurerm_linux_virtual_machine_scale_set.backend.id

  profile {
    name = "business-hours"
    capacity {
      default = 1
      minimum = 1
      maximum = 1
    }
    recurrence {
      timezone = "India Standard Time"
      days     = local.schedule_days
      hours    = [var.business_hours_start_hour]
      minutes  = [0]
    }
  }

  profile {
    name = "nightly-shutdown"
    capacity {
      default = 0
      minimum = 0
      maximum = 0
    }
    recurrence {
      timezone = "India Standard Time"
      days     = local.schedule_days
      hours    = [var.business_hours_stop_hour]
      minutes  = [var.business_hours_stop_minute]
    }
  }
}
