package com.bki.ot.ds.vault.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import com.bki.ot.ds.vault.util.Logger;

public class ParamStoreComponent extends LambdaComponent {

	private final Logger log = Logger.LOG;
	private AWSSimpleSystemsManagement ssmClient;

	@Override
	public void init() {
		ssmClient = AWSSimpleSystemsManagementClientBuilder.standard()
				.withRegion(region)
				.build();
	}

	public String getParam(String paramName) {
		return getParam(paramName, false);
	}

	public String getSecureParam(String paramName) {
		return getParam(paramName, true);
	}

	private String getParam(String paramName, boolean isSecure) {

		log.log(" - Getting param " + paramName + " (secure = " + isSecure + ")...");

		GetParameterRequest request = new GetParameterRequest()
				.withName(paramName)
				.withWithDecryption(isSecure);

		try {
			log.debug(" - Got param...");
			return ssmClient.getParameter(request).getParameter().getValue();

		} catch (ParameterNotFoundException e) {
			log.error("Parameter " + paramName + " was not found in parameter store");
			return null;
		}
	}

	// based on https://gist.github.com/davidrosenstark/4a33f2c0eab59d9d7e429bd1c20aea92
	public Map<String, String> getParametersByPath(String path, boolean encryption) {
		GetParametersByPathRequest getParametersByPathRequest = new GetParametersByPathRequest()
				.withPath(path)
				.withWithDecryption(encryption)
				.withRecursive(true);
		String token = null;
		Map<String, String> params = new HashMap<>();
		do {
			log.log("- Sending getParametersByPath request, path = " + path); // + ", search token = " + token + "...");
			getParametersByPathRequest.setNextToken(token);
			GetParametersByPathResult parameterResult = ssmClient.getParametersByPath
					(getParametersByPathRequest);
			log.debug(" - Got params...");
			token = parameterResult.getNextToken();
			params.putAll(addParamsToMap(path, parameterResult.getParameters()));
		} while (token != null);
		return params;
	}

	private Map<String,String> addParamsToMap(String path, List<Parameter> parameters) {
		return parameters.stream().map( param -> {
			// remove given path from parameter name:
			return new ImmutablePair<>(param.getName().replaceFirst(path + "\\/", ""), param.getValue());
		}).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
	}

}

