resource "azurerm_resource_group" "onesub" {
  name     = var.resource_group_name
  location = var.location
  tags = {
    application = "OneSub"
    environment = "three-tier"
    managed_by  = "terraform"
  }
}

resource "azurerm_virtual_network" "onesub" {
  name                = "${var.name_prefix}-vnet"
  address_space       = ["10.40.0.0/16"]
  location            = azurerm_resource_group.onesub.location
  resource_group_name = azurerm_resource_group.onesub.name
}
