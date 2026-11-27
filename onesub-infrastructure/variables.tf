variable "subscription_id" {
  description = "Explicit Azure subscription ID; no currently selected CLI subscription is assumed."
  type        = string

  validation {
    condition     = can(regex("^[0-9a-fA-F-]{36}$", var.subscription_id))
    error_message = "Set subscription_id to an Azure subscription UUID."
  }
}

variable "location" {
  description = "Azure region for the POC resources."
  type        = string
  default     = "centralindia"
}

variable "resource_group_name" {
  description = "Dedicated resource group so POC resources can be managed and budgeted together."
  type        = string
  default     = "onesub-poc-rg"
}

variable "name_prefix" {
  description = "Lowercase resource-name prefix."
  type        = string
  default     = "onesub-poc"

  validation {
    condition     = can(regex("^[a-z0-9][a-z0-9-]{1,18}[a-z0-9]$", var.name_prefix))
    error_message = "name_prefix must be 3-20 lowercase letters, digits, or hyphens and start/end with a letter or digit."
  }
}

variable "vm_size" {
  description = "Small, 2 GiB burstable VM; deallocate when not practicing."
  type        = string
  default     = "Standard_B1ms"
}

variable "admin_cidr" {
  description = "Your current public IPv4 address in /32 CIDR form for SSH access."
  type        = string

  validation {
    condition     = can(cidrhost(var.admin_cidr, 0)) && can(regex("^([0-9]{1,3}\\.){3}[0-9]{1,3}/32$", var.admin_cidr))
    error_message = "admin_cidr must be one valid IPv4 address with a /32 suffix."
  }
}

variable "ssh_public_key_path" {
  description = "Path to the SSH public key installed on the VM. The private key remains local."
  type        = string
  default     = "~/.ssh/id_rsa.pub"
}

variable "ssh_public_key" {
  description = "SSH public key supplied by automation. When unset, ssh_public_key_path is used."
  type        = string
  default     = null
}

variable "auto_shutdown_time" {
  description = "Daily VM deallocation time in 24-hour HHmm India Standard Time."
  type        = string
  default     = "2330"

  validation {
    condition     = can(regex("^([01][0-9]|2[0-3])[0-5][0-9]$", var.auto_shutdown_time))
    error_message = "auto_shutdown_time must use 24-hour HHmm format, for example 2330."
  }
}

variable "frontend_vm_size" {
  description = "VM size for the frontend scale set."
  type        = string
  default     = "Standard_B1s"
}

variable "backend_vm_size" {
  description = "VM size for the Spring Boot backend scale set."
  type        = string
  default     = "Standard_B1ms"
}

variable "domain_name" {
  description = "Apex domain registered at GoDaddy and served by the frontend."
  type        = string
  default     = "onesub.in"

  validation {
    condition     = can(regex("^[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)+$", var.domain_name))
    error_message = "domain_name must be a valid fully-qualified domain name."
  }
}

variable "postgres_admin_username" {
  description = "Administrator login for the new PostgreSQL Flexible Server."
  type        = string
  default     = "onesubadmin"
}

variable "postgres_admin_password" {
  description = "Administrator password supplied from Jenkins credentials."
  type        = string
  sensitive   = true
  default     = null
}

variable "postgres_sku_name" {
  description = "Burstable PostgreSQL Flexible Server SKU."
  type        = string
  default     = "B_Standard_B1ms"
}

variable "postgres_storage_mb" {
  description = "PostgreSQL storage allocation in MiB."
  type        = number
  default     = 32768

  validation {
    condition     = var.postgres_storage_mb >= 32768
    error_message = "PostgreSQL Flexible Server requires at least 32768 MiB of storage."
  }
}

variable "business_hours_start_hour" {
  description = "Scale the application VMSS instances up at this India Standard Time hour."
  type        = number
  default     = 8

  validation {
    condition     = var.business_hours_start_hour >= 0 && var.business_hours_start_hour <= 23
    error_message = "business_hours_start_hour must be between 0 and 23."
  }
}

variable "business_hours_stop_hour" {
  description = "Scale the application VMSS instances down at this India Standard Time hour."
  type        = number
  default     = 23

  validation {
    condition     = var.business_hours_stop_hour >= 0 && var.business_hours_stop_hour <= 23
    error_message = "business_hours_stop_hour must be between 0 and 23."
  }
}

variable "business_hours_stop_minute" {
  description = "Minute past the stop hour when the application VMSS instances scale to zero."
  type        = number
  default     = 30

  validation {
    condition     = var.business_hours_stop_minute >= 0 && var.business_hours_stop_minute <= 59
    error_message = "business_hours_stop_minute must be between 0 and 59."
  }
}

variable "enable_three_tier" {
  description = "Create the separate frontend/backend VMSS and managed PostgreSQL deployment."
  type        = bool
  default     = false
}
