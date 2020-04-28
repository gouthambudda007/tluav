###########################################################
# Local Variables

locals {
  service_name          = "Vault"
  is_prod               = var.Environment == null || var.Environment == "" ? true : false
  dash                  = local.is_prod ? "" : "-"
  name                  = "${var.Environment}${local.dash}${var.SellerDigitalSubdomainName}-service"
  fullDomain            = "${var.Environment}${local.dash}${var.SellerDigitalSubdomainName}.${var.RootUrl}"
  artifact_folder_path  = "./${local.is_prod ? "prod" : var.Environment}/${local.service_name}/artifacts/"
  web_acl_names         = [ for region in var.regions: "${var.web_acl_name_prefix}${region}" ]

}

# enable dependancy ---------------------------------------

resource "null_resource" "module_depends_on" {
  triggers = {
    value = length(var.module_depends_on)
  }
}

data "aws_acm_certificate" "ACMCert" {
  domain   = var.SSLCertDomain
}
# data references -----------------------------------------

data "aws_route53_zone" "hosted_zone" {
  zone_id = var.HostedZoneID
}

data "aws_wafregional_web_acl" "api_gateway_web_acl" {
  count = length(local.web_acl_names)

  name = local.web_acl_names[count.index]
}

data "aws_region" "current" {}

###########################################################

module "createbucket" {
  source  = "terraform.bkfs.com/BlackKnight/bki-s3-bucket/aws"
 # source  = "./modules/terraform-aws-bki-s3-bucket"
  version = "1.0.6"

  bucket_name = "${var.target-prefix}-${var.Environment}-${var.DEFAULT_BUCKET_ID}-${var.BUCKET_SUFFIX}"
  nologgingapproved_tag = "yes"
  function_tag = "bucket"
  env_tag = local.is_prod ? "prod" : var.Environment
  createdby_tag = "tfe"
  appid_tag = var.AppId
  awsaccount_tag = var.AccountName
}

###########################################################
# DynamoDB

module "dynamodb_document_info" {
  source  = "terraform.bkfs.com/BlackKnight/bki-dynamodb-table-prevent-delete/aws"
 # source  = "./modules/terraform-aws-bki-dynamodb-table-prevent-delete"
  version = "1.0.0"

  billing_mode      = "PAY_PER_REQUEST"
  name              = "${var.Environment}${local.dash}${var.app_name}-${var.SellerDigitalSubdomainName}-document-info"
  hash_key          = "documentId"
  ttl_attribute     = "DISABLED"
  # range_key         = "Time"
  #read_capacity     = 3
  #write_capacity    = 3

  dynamodb_attributes = [
    {
      name = "documentId"
      type = "S"
    },
    {
      name = "docStatus"
      type = "S"
    },
    {
      name = "correlationId"
      type = "S"
    },
    {
      name = "transactionId"
      type = "S"
    },
     {
      name = "docType"
      type = "S"
    },
    {
      name = "clientId"
      type = "S"
    }

  ]

     global_secondary_index_map = [
      {
        name               = "docStatus-index"
        hash_key           = "docStatus"
        range_key          = ""
        write_capacity     = null
        read_capacity      = null
        projection_type    = "ALL"
        non_key_attributes = []
      },
	    {
        name               = "correlationId-index"
        hash_key           = "correlationId"
        range_key          = ""
        write_capacity     = null
        read_capacity      = null
        projection_type    = "ALL"
        non_key_attributes = []
      },
	    {
        name               = "transactionId-index"
        hash_key           = "transactionId"
        range_key          = ""
        write_capacity     = null
        read_capacity      = null
        projection_type    = "ALL"
        non_key_attributes = []
      },
	    {
        name               = "docType-index"
        hash_key           = "docType"
        range_key          = ""
        write_capacity     = null
        read_capacity      = null
        projection_type    = "ALL"
        non_key_attributes = []
      },
	    {
        name               = "clientId-index"
        hash_key           = "clientId"
        range_key          = ""
        write_capacity     = null
        read_capacity      = null
        projection_type    = "ALL"
        non_key_attributes = []
      }
   ]


  appid_tag      = var.AppId
  awsaccount_tag = var.AccountName
  createdby_tag  = var.Creator
  env_tag        = local.is_prod ? var.client_num : var.Environment
  function_tag   = "User table"
}

module "dynamodb_event_history" {
  source  = "terraform.bkfs.com/BlackKnight/bki-dynamodb-table-prevent-delete/aws"
 # source  = "./modules/terraform-aws-bki-dynamodb-table-prevent-delete"
  version = "1.0.0"

  billing_mode      = "PAY_PER_REQUEST"
  name              = "${var.Environment}${local.dash}${var.app_name}-${var.SellerDigitalSubdomainName}-event-history"
  hash_key          = "documentId"
 # Primary sort key  = "eventTimestamp"
   range_key         = "eventTimestamp"
  #read_capacity     = 3
  #write_capacity    = 3

  dynamodb_attributes = [
    {
      name = "documentId"
      type = "S"
    },
    {
      name = "event"
      type = "S"
    }
  ]

  global_secondary_index_map = [
    {
      name               = "event-index"
      hash_key           = "event"
      range_key          = ""
      write_capacity     = null
      read_capacity      = null
      projection_type    = "ALL"
      non_key_attributes = []
    }
  ]
  
  appid_tag      = var.AppId
  awsaccount_tag = var.AccountName
  createdby_tag  = var.Creator
  env_tag        = local.is_prod ? var.client_num : var.Environment
  function_tag   = "Orgs table"
}


###########################################################

module "api-gateway" {
  source = "../modules/terraform-aws-bki-api-gateway"
  # source  = "terraform.bkfs.com/BlackKnight/bki-api-gateway/aws"
  # version = "1.0.2"
  
  api_gateway_name = "${var.Environment}${local.dash}${var.SellerDigitalSubdomainName}-api-gateway"
  xray_tracing_enabled = true
  # api_gateway_endpoint_type = "EDGE"
  api_gateway_endpoint_type = "REGIONAL"

  swagger_template_file = "${local.artifact_folder_path}BKI-OT-Vault-swagger-apigateway.json"
  #swagger_template_file = "./Vault/artifacts/BKI-OT-Vault-swagger-apigateway.json" 
  swagger_vars = {
    aws_region                    = data.aws_region.current.name
    full_domain                   = local.fullDomain
    title                         = local.name
    # stage                         = var.Stage
    stage                         = "Prod"
    ApiGatewayLambda_arn          = module.lambda_ApiGatewayLambda.arn
    #PresignedPostUrlGenerator_arn  = module.lambda_PresignedPostUrlGenerator.arn
    #VirusScanLambda_arn            = module.lambda_VirusScanLambda.arn
    #CleanupLambda_arn              = module.lambda_CleanupLambda.arn
    cognito_user_pool_arn           = module.bki-cognito-user-pool.arn
  }

  api_gateway_description = "API gateway for Seller Digital Identity Service"
  # api_gateway_stage       = var.Stage
  api_gateway_stage       = "Prod"
  env_tag                 = local.is_prod ? var.client_num : var.Environment
  awsaccount_tag          = var.AccountName
  appid_tag               = var.AppId
}

module "api-gateway-domain" {
  source  = "../modules/terraform-aws-bki-api-gateway-domain"
  # version = "1.0.0"
  
  domain_name = local.fullDomain
  # certificate_arn = data.aws_acm_certificate.ACMCert.arn
  regional_certificate_arn = data.aws_acm_certificate.ACMCert.arn
  security_policy = "TLS_1_2"
  # endpoint_types = ["EDGE"]
  endpoint_types = ["REGIONAL"]
}

module "api-gateway-base-path-mapping" {
 # source  = "./modules/terraform-aws-api-gateway-base-path-mapping"
  source = "terraform.bkfs.com/BlackKnight/bki-api-gateway-base-path-mapping/aws"
  version = "1.0.0"
  
  
  api_id      = module.api-gateway.api_id
  domain_name = module.api-gateway-domain.id
  stage_name  = module.api-gateway.stage_name
  #base_path   = "latest"
}

module "waf-regional-web-acl-association" {
  source = "../modules/terraform-aws-bki-wafregional-web-acl-association"
  # version = 1.0.0

  web_acl_id    = data.aws_wafregional_web_acl.api_gateway_web_acl[0].id
  resource_arn  = module.api-gateway.stage_arn
}

module "bki-route53-alias-record" {
  # source  = "./modules/terraform-aws-bki-route53-alias"
  source  = "terraform.bkfs.com/BlackKnight/bki-route53-alias/aws"
  version = "1.0.0"

  name = local.fullDomain
  # zone_id = var.HostedZoneID
  zone_id = data.aws_route53_zone.hosted_zone.zone_id

  #alias_name             = module.api-gateway-domain.cloudfront_domain_name
  alias_name               = module.api-gateway-domain.regional_domain_name
  #alias_zone_id          = "Z2FDTNDATAQYW2"
  alias_zone_id            = module.api-gateway-domain.regional_zone_id

  vpc_id = null
}

###########################################################
# Lambdas

module "LambdaExecutionRole" {
  source  = "terraform.bkfs.com/BlackKnight/bki-iam-role/aws"
  version = "1.0.5"
  # source  = "./modules/terraform-aws-bki-iam-role"

  role_name   = "${local.name}-lambda-role"
  role_description = "Role for ${local.name} lambdas"
  function_tag                = ""
  appid_tag                   = var.AppId
  env_tag                     = local.is_prod ? var.client_num : var.Environment
  awsaccount_tag              = var.AccountName
  createdby_tag	              = var.Creator

  assume_role_policy_inline   =<<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "sts:AssumeRole"
      ],
      "Effect": "Allow",
      "Principal": {
        "Service": [
          "lambda.amazonaws.com",
          "apigateway.amazonaws.com"
        ]
      }
    }
  ]
}
EOF
}

module "bki-iam-role-policy"{
  source  = "terraform.bkfs.com/BlackKnight/bki-iam-role-policy/aws"
  version = "1.0.3"
  # source = "./modules/terraform-aws-bki-iam-role-policy" 

  bki_iam_role_policy_name      = "bki-${local.name}-gateway-access-policy"  # TODO: rename
  bki_iam_role_policy_role_name = module.LambdaExecutionRole.name
  inline_policy                 =<<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "lambda:*",
        "apigateway:*",
        "cognito-idp:*",
        "userpool:*",
        "ec2:*",
        "s3:*",
        "ssm:*",
        "dynamodb:*",
        "cloudwatch:*",
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "xray:*"
      ],
      "Resource": "*"
    }
  ]
}
EOF
}

#########################################################################

module "notification1" {
  source = "../modules/terraform-aws-bki-s3-bucket-notification"
  bucket = module.createbucket.id
  lambda_function = [{
  lambda_function_arn = module.lambda_VirusScanLambda.arn
  events = ["s3:ObjectCreated:*"]
  }]
}

#########################################################################

module "terraform-aws-lambda-permission-ApiGatewayLambda" {
  source  = "terraform.bkfs.com/BlackKnight/bki-lambda-permission/aws"
  version = "1.0.3"
  # source  = "./modules/terraform-aws-bki-lambda-permission"

  lambda_permission_statement_id  = "AllowExecutionFromAPIGateway"
  lambda_permission_action        = "lambda:InvokeFunction"
  lambda_permission_function_name = module.lambda_ApiGatewayLambda.lambda_function_name
  lambda_permission_principal     = "apigateway.amazonaws.com"
  lambda_permission_source_arn    = "${module.api-gateway.execution_arn}*/*"
}

module "lambda_ApiGatewayLambda" {
  source  = "terraform.bkfs.com/BlackKnight/bki-lambda/aws"
  version = "1.0.8"
  # source  = "./modules/terraform-aws-bki-lambda"

  lambda_function_name    = "${var.Environment}${local.dash}vault-ApiGatewayLambda"
  lambda_source_file_name = "${local.artifact_folder_path}bki-ot-ds-vault-${var.build_number}.jar"
  # lambda_source_file_name = "${local.artifact_folder_path}bki-ot-ds-vault-1.0.0.jar"
  #lambda_source_file_name = "./Vault/artifacts/bki-ot-ds-vault-1.0.0.jar"
  lambda_runtime          = "java8"
  lambda_memory           = 512
  lambda_timeout          = 30
  lambda_role_arn         = module.LambdaExecutionRole.arn
  vpc_subnet_ids          = var.subnets
  security_group_id       = [var.lambda_sg_id]
  enable_xray             = true  # default: false
  lambda_function_description = "Handles the basic request"
  lambda_handler_name         = "com.bki.ot.ds.vault.lambda.VaultApiGatewayLambda::handleRequest"
  env_variables = {
    EXPIRATION_SECONDS    = var.EXPIRATION_SECONDS
    ENV_NAME              = var.Environment
    BUCKET_PREFIX         = var.BUCKET_PREFIX
    BUCKET_SUFFIX         = var.BUCKET_SUFFIX
    DEFAULT_BUCKET_ID     = var.DEFAULT_BUCKET_ID
  }

  name_tag                = "${local.name}-ApiGateway-Lambda"
  appid_tag               = var.AppId
  env_tag                 = local.is_prod ? var.client_num : var.Environment
  awsaccount_tag          = var.AccountName
  createdby_tag           = var.Creator
  function_tag            = "Handles the basic request"
}

module "terraform-aws-lambda-permission-PresignedPostUrlGenerator" {
  source  = "terraform.bkfs.com/BlackKnight/bki-lambda-permission/aws"
  version = "1.0.3"
  # source  = "./modules/terraform-aws-bki-lambda-permission"

  lambda_permission_statement_id  = "AllowExecutionFromAPIGateway"
  lambda_permission_action        = "lambda:InvokeFunction"
  lambda_permission_function_name = module.lambda_PresignedPostUrlGenerator.lambda_function_name
  lambda_permission_principal     = "apigateway.amazonaws.com"
  lambda_permission_source_arn    = "${module.api-gateway.execution_arn}*/*"
}

module "lambda_PresignedPostUrlGenerator" {
  source  = "terraform.bkfs.com/BlackKnight/bki-lambda/aws"
  version = "1.0.8"
  #source  = "./modules/terraform-aws-bki-lambda"

  lambda_function_name    = "${var.Environment}${local.dash}vault-PresignedPostUrlGenerator"
  # lambda_data_archive_file_type = "zip"
  # lambda_output_file_name = "ui.zip"
  lambda_source_file_name = "${local.artifact_folder_path}presigned_post.zip"
  # lambda_source_file_name = "./Vault/artifacts/presigned_post.zip"
  lambda_runtime          = "python3.7"
  lambda_memory            = 1536
  lambda_timeout          = 30
  lambda_role_arn         = module.LambdaExecutionRole.arn
  vpc_subnet_ids          = var.subnets
  security_group_id       = [var.lambda_sg_id]
  enable_xray             = true  # default: false
  lambda_function_description = "Handles the basic request"
  lambda_handler_name         = "presigned_post.lambda_handler"
  env_variables = {
    #  AccountOwner            = ""
    MAX_FILE_SIZE = 35000000
  }

  name_tag                = "${local.name}-PresignedPostUrlGenerator"
  appid_tag               = var.AppId
  env_tag                 = local.is_prod ? var.client_num : var.Environment
  awsaccount_tag          = var.AccountName
  createdby_tag           = var.Creator
  function_tag            = "Handles the basic request"
}

module "terraform-aws-lambda-permission-VirusScanLambda" {
 source  = "terraform.bkfs.com/BlackKnight/bki-lambda-permission/aws"
 version = "1.0.3"
  # source  = "./modules/terraform-aws-bki-lambda-permission"

  lambda_permission_statement_id  = "AllowExecutionFromAPIGateway"
  lambda_permission_action        = "lambda:InvokeFunction"
  lambda_permission_function_name = module.lambda_VirusScanLambda.lambda_function_name
  lambda_permission_principal     = "apigateway.amazonaws.com"
  lambda_permission_source_arn = "${module.api-gateway.execution_arn}*/*"
}

module "lambda_VirusScanLambda" {
  source  = "terraform.bkfs.com/BlackKnight/bki-lambda/aws"
  version = "1.0.8"
  # source  = "./modules/terraform-aws-bki-lambda"

  lambda_function_name    = "${var.Environment}${local.dash}vault-VirusScanLambda"
  lambda_source_file_name = "${local.artifact_folder_path}bki-ot-ds-vault-${var.build_number}.jar"
  # lambda_source_file_name = "${local.artifact_folder_path}bki-ot-ds-vault-1.0.0.jar"
  # lambda_source_file_name	= "./Vault/artifacts/bki-ot-ds-vault-1.0.0.jar"
  lambda_runtime          = "java8"
  lambda_memory           = 512
  lambda_timeout          = 30
  lambda_role_arn         = module.LambdaExecutionRole.arn
  vpc_subnet_ids          = var.subnets
  security_group_id       = [var.lambda_sg_id]
  enable_xray             = true  # default: false
  lambda_function_description = "Handles the basic request"
  lambda_handler_name         = "com.bki.ot.ds.vault.lambda.VirusScanLambda::handleRequest"
  env_variables = {
    SCAN_ENGINE_IP    = var.SCAN_ENGINE_IP
    SCAN_ENGINE_PORT  = var.SCAN_ENGINE_PORT
    ENV_NAME          = var.Environment
  }

  name_tag                = "${local.name}-VirusScanLambda"
  appid_tag               = var.AppId
  env_tag                 = local.is_prod ? var.client_num : var.Environment
  awsaccount_tag          = var.AccountName
  createdby_tag           = var.Creator
  function_tag            = "Handles the basic request"
}

module "terraform-aws-lambda-permission-CleanupLambda" {
 source  = "terraform.bkfs.com/BlackKnight/bki-lambda-permission/aws"
 version = "1.0.3"
  # source  = "./modules/terraform-aws-bki-lambda-permission"

  lambda_permission_statement_id  = "AllowExecutionFromAPIGateway"
  lambda_permission_action        = "lambda:InvokeFunction"
  lambda_permission_function_name = module.lambda_CleanupLambda.lambda_function_name
  lambda_permission_principal     = "apigateway.amazonaws.com"
  lambda_permission_source_arn = "${module.api-gateway.execution_arn}*/*"
}

module "lambda_CleanupLambda" {
  source  = "terraform.bkfs.com/BlackKnight/bki-lambda/aws"
  version = "1.0.8"
  # source  = "./modules/terraform-aws-bki-lambda"

  lambda_function_name    = "${var.Environment}${local.dash}Vault-CleanupLambda"
  lambda_source_file_name = "${local.artifact_folder_path}bki-ot-ds-vault-${var.build_number}.jar"
  # lambda_source_file_name = "${local.artifact_folder_path}bki-ot-ds-vault-1.0.0.jar"
  #lambda_source_file_name	= "./Vault/artifacts/bki-ot-ds-vault-1.0.0.jar"
  lambda_runtime          = "java8"
  lambda_memory           = 512
  lambda_timeout          = 30
  lambda_role_arn         = module.LambdaExecutionRole.arn
  vpc_subnet_ids          = var.subnets
  security_group_id       = [var.lambda_sg_id]
  enable_xray             = true  # default: false
  lambda_function_description = "Handles the basic request"
  lambda_handler_name         = "com.bki.ot.ds.vault.lambda.CleanupLambda::handleRequest"
  #env_variables = {
  #  RootUrl           = ""
  #}

  name_tag                = "${local.name}-CleanupLambda"
  appid_tag               = var.AppId
  env_tag                 = local.is_prod ? var.client_num : var.Environment
  awsaccount_tag          = var.AccountName
  createdby_tag           = var.Creator
  function_tag            = "Handles the basic request"
}

