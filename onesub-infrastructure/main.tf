resource "random_id" "dns_suffix" {
  byte_length = 3
}

resource "azurerm_resource_group" "onesub" {
  name     = var.resource_group_name
  location = var.location
  tags = {
    application = "OneSub"
    environment = "poc"
    managed_by  = "terraform"
  }
}

resource "azurerm_virtual_network" "onesub" {
  name                = "${var.name_prefix}-vnet"
  address_space       = ["10.40.0.0/16"]
  location            = azurerm_resource_group.onesub.location
  resource_group_name = azurerm_resource_group.onesub.name
}

resource "azurerm_subnet" "onesub" {
  name                 = "${var.name_prefix}-subnet"
  resource_group_name  = azurerm_resource_group.onesub.name
  virtual_network_name = azurerm_virtual_network.onesub.name
  address_prefixes     = ["10.40.1.0/24"]
}

resource "azurerm_network_security_group" "onesub" {
  name                = "${var.name_prefix}-nsg"
  location            = azurerm_resource_group.onesub.location
  resource_group_name = azurerm_resource_group.onesub.name

  security_rule {
    name                       = "ssh-from-admin-only"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = var.admin_cidr
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "http"
    priority                   = 110
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "80"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "https"
    priority                   = 120
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
}

resource "azurerm_public_ip" "onesub" {
  name                = "${var.name_prefix}-ip"
  location            = azurerm_resource_group.onesub.location
  resource_group_name = azurerm_resource_group.onesub.name
  allocation_method   = "Static"
  sku                 = "Standard"
  domain_name_label   = "${var.name_prefix}-${random_id.dns_suffix.hex}"
}

resource "azurerm_network_interface" "onesub" {
  name                = "${var.name_prefix}-nic"
  location            = azurerm_resource_group.onesub.location
  resource_group_name = azurerm_resource_group.onesub.name

  ip_configuration {
    name                          = "primary"
    subnet_id                     = azurerm_subnet.onesub.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.onesub.id
  }
}

resource "azurerm_network_interface_security_group_association" "onesub" {
  network_interface_id      = azurerm_network_interface.onesub.id
  network_security_group_id = azurerm_network_security_group.onesub.id
}

resource "azurerm_linux_virtual_machine" "onesub" {
  name                              = "${var.name_prefix}-vm"
  computer_name                     = "onesub-poc"
  location                          = azurerm_resource_group.onesub.location
  resource_group_name               = azurerm_resource_group.onesub.name
  size                              = var.vm_size
  admin_username                    = "onesubadmin"
  disable_password_authentication   = true
  network_interface_ids             = [azurerm_network_interface.onesub.id]
  custom_data                       = base64encode(file("${path.module}/cloud-init.yaml"))
  patch_mode                        = "AutomaticByPlatform"
  provision_vm_agent                = true
  vm_agent_platform_updates_enabled = true

  admin_ssh_key {
    username   = "onesubadmin"
    public_key = var.ssh_public_key != null ? var.ssh_public_key : file(pathexpand(var.ssh_public_key_path))
  }

  os_disk {
    name                 = "${var.name_prefix}-osdisk"
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

  boot_diagnostics {}

  tags = azurerm_resource_group.onesub.tags
}

resource "azurerm_dev_test_global_vm_shutdown_schedule" "onesub" {
  virtual_machine_id    = azurerm_linux_virtual_machine.onesub.id
  location              = azurerm_resource_group.onesub.location
  enabled               = true
  daily_recurrence_time = var.auto_shutdown_time
  timezone              = "India Standard Time"

  notification_settings {
    enabled         = false
    time_in_minutes = 30
  }
}
