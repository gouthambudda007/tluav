output "APIDomainName" {
  description = "api domain"
  value = module.api-gateway-domain.regional_domain_name
}
output "Lambda_ApiGatewayLambda_Name" {
  description = "Name of ApiGatewayLambda Lambda"
  value = module.lambda_ApiGatewayLambda.lambda_function_name
}
