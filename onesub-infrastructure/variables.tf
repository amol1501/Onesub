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
