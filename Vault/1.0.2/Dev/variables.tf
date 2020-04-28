variable "Stage" {
  description = "dev, test, prod"
  type = string
  default = "_stage_"
}

variable "Environment" {
  description = "environment designation"
  type        = string
}

variable "client" {
  description = "client; account owner"
  type        = string
}

variable "client_num" {
  description = "environment designation"
  type        = string
}

variable "AppId" {
  description = "application ID"
  type        = string
}

variable "app_name" {
  description = "application ID"
  type        = string
}

variable "Creator" {
  description = "created_by tag value"
  type        = string
}

variable "AccountName" {
  description = "aws_account tag value"
  type        = string
}

variable "SellerDigitalSubdomainName" {
  description = "Subdomain name for this setup"
  type        = string
}


#variable "LambdaCustomMessage" {
#  description = ""
#  type = string
#}

variable "lambda_sg_id" {
  description = "Security Group for lambda egress"
  type = string
}

# enable module dependency
variable "module_depends_on" {
  description = "enables the passing of ofther modules to force dependency"
  type = list
  default = []
}

variable "build_number" {
  description = "The version and build number of any given build (e.g. “1.0.1.11940”). Defined at build time"
  type        = string
  default     = "<build#>"
}

#variable "security_group_id" {}

variable "HostedZoneID" {}

variable "target-prefix" {}

variable "SSLCertDomain" {}

variable "RootUrl" {}

variable "subnets" {
  type = list
}

variable "IdentityService_UserPoolArn" {
  description = "Cognito user pool arn from Identity Service"
  type = string
}

variable "web_acl_name_prefix" {
  description = "The standard prefix of the names of the Web ACLs in all regions"
  type        = string
  default     = ""
}

variable "BUCKET_SUFFIX" {
  description = "bucket suffix"
  type = string
  default = "sellerdigital-vault"
}

variable "EXPIRATION_SECONDS" {
  description = "Expiration seconds"
  type = string
  default = "10"
}
variable "BUCKET_PREFIX" {
  description = "bucket prefix"
  type = string
  default = "bki-ot"
}

variable "DEFAULT_BUCKET_ID" {
  description = "bucket id"
  type = string
  default = "docs"
}

variable "SCAN_ENGINE_IP" {
  description = "scan engine ip"
  type = string
  default = "spe.shared-services.awsdevint.site"
}

variable "SCAN_ENGINE_PORT" {
  description = "port number"
  type = string
  default = "1344"
}