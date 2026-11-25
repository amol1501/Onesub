resource "azurerm_subnet" "artifactory" {
  name                 = "${var.name_prefix}-jfrog-subnet"
  resource_group_name  = azurerm_resource_group.onesub.name
  virtual_network_name = azurerm_virtual_network.onesub.name
  address_prefixes     = ["10.40.2.0/24"]
}

resource "azurerm_network_security_group" "artifactory" {
  name                = "${var.name_prefix}-jfrog-nsg"
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
    name                       = "artifactory-from-vnet-only"
    priority                   = 110
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "8082"
    source_address_prefix      = "10.40.0.0/16"
    destination_address_prefix = "*"
  }
}

resource "azurerm_public_ip" "artifactory" {
  name                = "${var.name_prefix}-jfrog-ip"
  location            = azurerm_resource_group.onesub.location
  resource_group_name = azurerm_resource_group.onesub.name
  allocation_method   = "Static"
  sku                 = "Standard"
  domain_name_label   = "${var.name_prefix}-jfrog-${random_id.dns_suffix.hex}"
}

resource "azurerm_network_interface" "artifactory" {
  name                = "${var.name_prefix}-jfrog-nic"
  location            = azurerm_resource_group.onesub.location
  resource_group_name = azurerm_resource_group.onesub.name

  ip_configuration {
    name                          = "primary"
    subnet_id                     = azurerm_subnet.artifactory.id
    private_ip_address_allocation = "Static"
    private_ip_address            = "10.40.2.4"
    public_ip_address_id          = azurerm_public_ip.artifactory.id
  }
}

resource "azurerm_network_interface_security_group_association" "artifactory" {
  network_interface_id      = azurerm_network_interface.artifactory.id
  network_security_group_id = azurerm_network_security_group.artifactory.id
}

resource "azurerm_linux_virtual_machine" "artifactory" {
  name                            = "${var.name_prefix}-jfrog"
  computer_name                   = "onesub-jfrog"
  location                        = azurerm_resource_group.onesub.location
  resource_group_name             = azurerm_resource_group.onesub.name
  size                            = var.artifactory_vm_size
  admin_username                  = "onesubadmin"
  disable_password_authentication = true
  network_interface_ids           = [azurerm_network_interface.artifactory.id]
  custom_data                     = base64encode(file("${path.module}/artifactory-cloud-init.yaml"))
  patch_mode                      = "AutomaticByPlatform"
  provision_vm_agent              = true

  admin_ssh_key {
    username   = "onesubadmin"
    public_key = var.ssh_public_key != null ? var.ssh_public_key : file(pathexpand(var.ssh_public_key_path))
  }

  os_disk {
    name                 = "${var.name_prefix}-jfrog-osdisk"
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

resource "azurerm_managed_disk" "artifactory_data" {
  name                 = "${var.name_prefix}-jfrog-data"
  location             = azurerm_resource_group.onesub.location
  resource_group_name  = azurerm_resource_group.onesub.name
  storage_account_type = "StandardSSD_LRS"
  create_option        = "Empty"
  disk_size_gb         = var.artifactory_data_disk_size_gb
  os_type              = "Linux"

  tags = azurerm_resource_group.onesub.tags
}

resource "azurerm_virtual_machine_data_disk_attachment" "artifactory_data" {
  managed_disk_id    = azurerm_managed_disk.artifactory_data.id
  virtual_machine_id = azurerm_linux_virtual_machine.artifactory.id
  lun                = 0
  caching            = "ReadWrite"
}
