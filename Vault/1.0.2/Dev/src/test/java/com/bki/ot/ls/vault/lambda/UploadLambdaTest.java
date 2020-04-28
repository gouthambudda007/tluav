package com.bki.ot.ls.vault.lambda;

import static com.bki.ot.ds.vault.util.Config.getFromConfig;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.bki.ot.ds.vault.api.FileUploadPostResponse;
import com.bki.ot.ds.vault.api.gateway.ApiGatewayProxyRequest;
import com.bki.ot.ds.vault.api.gateway.ApiGatewayProxyResponse;
import com.bki.ot.ds.vault.api.gateway.Identity;
import com.bki.ot.ds.vault.api.gateway.RequestContext;
import com.bki.ot.ds.vault.components.ComponentsProvider;
import com.bki.ot.ds.vault.components.DynamoDBComponent;
import com.bki.ot.ds.vault.components.ParamStoreComponent;
import com.bki.ot.ds.vault.components.PresigendPostComponent;
import com.bki.ot.ds.vault.components.S3Component;
import com.bki.ot.ds.vault.components.STSComponent;
import com.bki.ot.ds.vault.components.data.PresignedPostData;
import com.bki.ot.ds.vault.components.lambda.BaseLambda;
import com.bki.ot.ds.vault.dynamo.DocumentEvent;
import com.bki.ot.ds.vault.dynamo.DocumentInfo;
import com.bki.ot.ds.vault.dynamo.DocumentStatus;
import com.bki.ot.ds.vault.dynamo.EventHistory;
import com.bki.ot.ds.vault.lambda.VaultApiGatewayLambda;
import com.bki.ot.ls.vault.lambda.launcher.LauncherUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("serial")
//@RunWith(MockitoJUnitRunner.class)
public class UploadLambdaTest {
	
	// initialize default data to be used in the test: ----
	private String tokenIssuer = "aiva";
	private String tokenUser = "user134";
	private String tokenSubject = "subject123";
	private String tokenClient = "945";
	
	private String testAccount = "test aws account";
	private String testUrl = "https://testUrl.com";
	private String sourceIp = "206.201.77.151";
	private String fileName = "loan-application-01.pdf";
	private int fileSize = 12345678;
	private String corelationId = "1234567";
	private String transactionId = "TX1234567";
	private String docType = "W2";
	private String mimeType = "application/pdf";
	
	private Map<String, Object> sourceData = new HashMap<String, Object>() {{ put("key1", "value1"); put("key2", "value2"); }};
	private Map<String, String> fieldsData = new HashMap<String, String>() {{ put("field1", "data1"); put("field2", "data2"); }};
	
	// other variables and components: --------------------
	private ComponentsProvider componentsProviderMock;
	private BaseLambda<ApiGatewayProxyRequest, ApiGatewayProxyResponse> lambdaUnderTest;
	
	private PresignedPostData presignedPostData;

	private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	private Map<String, String> rawJwtKeys = new HashMap<>(); 
	private String bucketName;
	private String documentId;

	//@Rule
	//public MockitoRule rule = MockitoJUnit.rule();

	@Before
	public void setUp() throws Exception {
		//MockitoAnnotations.initMocks(this);

		LauncherUtils.initEnvVars();//TODO bring in

		componentsProviderMock = new ComponentsProviderMock().init();

		bucketName = getFromConfig("BUCKET_PREFIX") + "-" + 
				getFromConfig("ENV_NAME") + "-" + 
				getFromConfig("DEFAULT_BUCKET_ID") + "-" + 
				getFromConfig("BUCKET_SUFFIX", "");

		String rawKey = FileUtils.readFileToString(new File("src/main/resources/test-keys-01-public.pem"), StandardCharsets.UTF_8);
		rawJwtKeys.put("aiva", rawKey);

		// set up param store mock to return the public key (needed to initialize the lambda):
		ParamStoreComponent paramStoreMock = componentsProviderMock.get("paramStore", ParamStoreComponent.class);
		doReturn(rawJwtKeys).when(paramStoreMock).getParametersByPath("/" + System.getenv("ENV_NAME") + "/vault/jwt/key", true);

		presignedPostData = PresignedPostData.builder()
				.url(testUrl)
				.fields(fieldsData)
				.build();

		lambdaUnderTest = new VaultApiGatewayLambda() {
			@Override
			protected ComponentsProvider getComponentProvider() {
				return componentsProviderMock;
			}
		};
		
		// verify param store value was read:
		verify(paramStoreMock).getParametersByPath("/" + System.getenv("ENV_NAME") + "/vault/jwt/key", true);
	}

	@Test
	public void happyPath() throws Exception {

		// init mock components:
		S3Component s3Mock = componentsProviderMock.get("s3", S3Component.class);
		STSComponent stsMock = componentsProviderMock.get("sts", STSComponent.class);
		DynamoDBComponent dynamoMock = componentsProviderMock.get("dynamo", DynamoDBComponent.class);
		PresigendPostComponent presignedPostMock = componentsProviderMock.get("presigned", PresigendPostComponent.class);

		// setup mocks/spies:
		doReturn(presignedPostData).when(presignedPostMock).presignedPostUrlFor(anyString(), anyString());

		doReturn(true).when(s3Mock).bucketExists(anyString());
		doReturn(testAccount).when(stsMock).getAccount();

		ArgumentCaptor<Object> savedItemCaptor = ArgumentCaptor.forClass(Object.class);
		doNothing().when(dynamoMock).save(any(DocumentInfo.class));

		// create default request:
		ApiGatewayProxyRequest request = createRequest();

		// run through Lambda:
		ApiGatewayProxyResponse apiResponse = lambdaUnderTest.handleRequest(request, new TestContext());
		assertEquals(HttpStatus.SC_OK, apiResponse.getStatusCode());

		// verify response:
		FileUploadPostResponse response = jsonToObject(apiResponse.getBody(), FileUploadPostResponse.class);
		documentId = response.getDocumentId();
		assertEquals(36, documentId.length());
		
		FileUploadPostResponse expected = FileUploadPostResponse.builder()
				.documentId(documentId)
				.url(testUrl)
				.awsAccount(testAccount)
				.bucketName(bucketName)
				.objectKey(documentId)
				.errorMessage(null)
				.fields(fieldsData)
				.build();
		assertSimilarObjects(expected, response);
		//assertTrue(EqualsBuilder.reflectionEquals(expected,response));

		// verify spy calls: 
		verify(presignedPostMock).presignedPostUrlFor(bucketName, documentId);

		// verify saving docInfo and eventHistory:
		verify(dynamoMock, times(4)).save(savedItemCaptor.capture());
		
		// verify database writes:
		List<Object> capturedDocInfo = savedItemCaptor.getAllValues();
		assertEquals(DocumentInfo.class, capturedDocInfo.get(0).getClass());
		assertEquals(EventHistory.class, capturedDocInfo.get(1).getClass());
		assertEquals(DocumentInfo.class, capturedDocInfo.get(2).getClass());
		assertEquals(EventHistory.class, capturedDocInfo.get(3).getClass());
		verifyDocInfo((DocumentInfo) capturedDocInfo.get(2)); // (the first entry was overriden by this one)
		verifyFirstUpdate((EventHistory) capturedDocInfo.get(1));
		verifySecondUpdate((EventHistory) capturedDocInfo.get(3));
	}

	private void verifyDocInfo(DocumentInfo documentInfo) throws Exception {
		DocumentInfo expected = DocumentInfo.builder()
		.bucket(bucketName)
		.clientId(tokenClient)
		.correlationId(corelationId)
		.docSize(fileSize)
		.docSource(tokenIssuer)
		.docStatus(DocumentStatus.URL_GENERATED)
		.docType(docType)
		.documentId(documentId)
		.fileName(fileName)
		.mimeType(mimeType)
		.sourceData(sourceData)
		.transactionId(transactionId)
		.build();
		
		assertSimilarObjects(expected, documentInfo);
	}

	private void verifyFirstUpdate(EventHistory eventHistory) throws Exception {
		EventHistory expected = EventHistory.builder()
				.documentId(documentId)
				.event(DocumentEvent.UPLOAD_POST_REQUESTED)
				.sourceUser(tokenUser)
				.ipAddress(sourceIp)
				.eventStatus(true)
				.eventSource(tokenClient)
				.reason("")
				.columnName("")
				.afterValue("")
				.beforeValue("")
				// this field is not tested:
				.eventTimestamp(eventHistory.getEventTimestamp())
				.build();
				
		assertSimilarObjects(expected, eventHistory);
	}

	private void verifySecondUpdate(EventHistory eventHistory) throws Exception {
		EventHistory expected = EventHistory.builder()
				.documentId(documentId)
				.event(DocumentEvent.URL_GENERATED)
				.sourceUser(tokenUser)
				.ipAddress(sourceIp)
				.eventStatus(true)
				.eventSource(tokenClient)
				.reason("")
				.columnName("docStatus")
				.afterValue("URL_GENERATED")
				.beforeValue("UPLOAD_POST_REQUESTED")
				// this field is not tested:
				.eventTimestamp(eventHistory.getEventTimestamp())
				.build();
		
		assertSimilarObjects(expected, eventHistory);
	}

	private ApiGatewayProxyRequest createRequest() throws Exception {
		ApiGatewayProxyRequest request = new ApiGatewayProxyRequest();

		String token = LauncherUtils.generateJwt(tokenIssuer, tokenUser, tokenSubject, tokenClient);
		String requestBody = "{\"documentId\":\"\", \"fileName\":\"" + fileName + "\", \"fileSize\":" + fileSize + ", \"correlationId\":\"" + corelationId + "\", \"transactionId\":\"" + transactionId + "\", \"docType\":\"" + docType + "\", \"mimeType\":\"" + mimeType + "\", \"additionalData\":"+ objectToJson(sourceData) + "}";

		
		request.setResource("/upload");
		request.setHttpMethod("POST");
		request.setBody(requestBody);
		request.setHeaders(new HashMap<String, String>() {{ 
			put("Authorization","Bearer " + token);
		}});

		// add source IP:
		Identity identity = new Identity();
		identity.setSourceIp(sourceIp);
		RequestContext requestContext = new RequestContext();
		requestContext.setIdentity(identity);
		request.setRequestContext(requestContext);
		return request;
	}

	// utils: -----------------------------------------------------------------

	private void assertSimilarObjects(Object o1, Object o2) throws Exception {
		assertEquals(objectToJson(o1), objectToJson(o2));
	}
	
	private <T> T jsonToObject(String str, Class<T> valueType) throws Exception {
		return mapper.readValue(str, valueType);
	}

	private <T> String objectToJson(T object) throws Exception {
		return mapper.writeValueAsString(object);
	}
	
	//	{
	//		  "documentId" : "d96ff650-922e-4588-b1c5-49e19d769339",
	//		  "url" : "https://testUrl.com",
	//		  "awsAccount" : null,
	//		  "bucketName" : "bki-ot-sb1-usb-docs-sellerdigital-vault",
	//		  "objectKey" : "d96ff650-922e-4588-b1c5-49e19d769339",
	//		  "fields" : {
	//		    "field1" : "data1"
	//		  },
	//		  "errorMessage" : null
	//		}
}