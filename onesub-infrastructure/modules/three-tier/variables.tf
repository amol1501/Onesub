variable "resource_group_name" {
  type = string
}

variable "location" {
  type = string
}

variable "virtual_network_name" {
  type = string
}

variable "tags" {
  type = map(string)
}

variable "name_prefix" {
  type = string
}

variable "admin_cidr" {
  type = string
}

variable "ssh_public_key" {
  type     = string
  default  = null
  nullable = true
}

variable "ssh_public_key_path" {
  type = string
}

variable "domain_name" {
  type = string
}

variable "frontend_vm_size" {
  type = string
}

variable "backend_vm_size" {
  type = string
}

variable "postgres_admin_username" {
  type = string
}

variable "postgres_admin_password" {
  type      = string
  sensitive = true
  default   = null
  nullable  = true
}

variable "postgres_sku_name" {
  type = string
}

variable "postgres_storage_mb" {
  type = number
}

variable "business_hours_start_hour" {
  type = number
}

variable "business_hours_stop_hour" {
  type = number
}

variable "business_hours_stop_minute" {
  type = number
}
