package com.bki.ot.ds.vault.components.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.bki.ot.ds.vault.api.gateway.ApiGatewayProxyRequest;
import com.bki.ot.ds.vault.api.gateway.ApiGatewayProxyResponse;
import com.bki.ot.ds.vault.exception.AuthenticationException;
import com.bki.ot.ds.vault.exception.BadRequestException;
import com.bki.ot.ds.vault.exception.ProcessingException;

public abstract class ApiGatewayLambda extends BaseLambda<ApiGatewayProxyRequest , ApiGatewayProxyResponse> {

	@Override
	public ApiGatewayProxyResponse handleRequest(ApiGatewayProxyRequest request, Context context)  {

		log.debug("-- Initialize loging...");//TODO remove
		log.init(context);
		log.log("---------------------- Starting handleRequest()...");
		log.prettyPrint("Request", request);

		try {
			RestRequest dispatchInfo = RestRequest.fromRequest(request);
			log.log("dipatch info = " + dispatchInfo);
			return dispatchRequest(dispatchInfo, context);

		}  catch (AuthenticationException e) {
			log.error("-------- Failed authentication", e);
			return ApiGatewayProxyResponse.authError(e.getMessage());
			//return ApiGatewayProxyResponse.authError(""); // TODO - use this to not provide info
			
		} catch (BadRequestException e) {
			log.error("-------- Completed operation with error(s) due to bad request", e);
			return ApiGatewayProxyResponse.badRequest(e.getMessage());

		}  catch (ProcessingException e) {
			log.error("-------- Completed operation with processing error(s)", e);
			return ApiGatewayProxyResponse.internalError(e.getMessage());

		} catch (Exception e) {
			log.error("failure detected while handling request", e);
			log.log("-------- Completed operation with error(s)");
			return ApiGatewayProxyResponse.internalError("Internal server error: " + e.getMessage());//TODO mask message
		}
	}

	protected abstract ApiGatewayProxyResponse dispatchRequest(RestRequest dispatchInfo, Context context) throws Exception;

	protected void handleBadRequest(String errorMessage) {
		handleBadRequest(errorMessage, errorMessage);
	}

	protected void handleBadRequest(String errorMessage, String responseErrorMessage) {
		log.error(errorMessage);
		throw new BadRequestException(responseErrorMessage);
	}
}
