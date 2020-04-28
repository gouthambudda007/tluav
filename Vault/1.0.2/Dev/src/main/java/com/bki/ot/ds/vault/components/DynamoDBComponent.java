package com.bki.ot.ds.vault.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.bki.ot.ds.vault.util.Config;
import com.bki.ot.ds.vault.util.Logger;

public class DynamoDBComponent extends LambdaComponent {

	private final static String envName = Config.getFromConfig("ENV_NAME");
	private final Logger log = Logger.LOG;
	
	protected AmazonDynamoDB dynamoDBClient;
	protected DynamoDBMapper dbMapper;
	//protected ObjectMapper mapper;//TODO needed?

	@Override
	public void init() {
		dynamoDBClient = AmazonDynamoDBClientBuilder.standard()
				.withRegion(region)
				.build();
		//mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		dbMapper = new DynamoDBMapper(dynamoDBClient, new EnvTableNameResolver().config());
	}

	public <T> void save(T item) {
		log.log("Inserting item " + item + "...");
		dbMapper.save(item);
		log.log("Item saved...");//TODO
	}
	
    public <T> PaginatedQueryList<T> query(Class<T> clazz, DynamoDBQueryExpression<T> queryExpression) {
		PaginatedQueryList<T> items = dbMapper.query(clazz, queryExpression);
		return items;
	}

//	public void addItem(String tableName, Map<String, AttributeValue> item) {
//		log.log("Inserting item " + item + " to table " + tableName + "...");
//		
//		PutItemRequest request = new PutItemRequest(tableName, item);
//		PutItemResult result = dynamoDBClient.putItem(request);
//		
//		log.log("Result = " + result);
//	}


	public List<Map<String,AttributeValue>> scanItems(String tableName, HashMap<String, Condition> scanFilter) {
		log.log("Scanning table " + tableName + " with filter " + scanFilter + "...");

		ScanRequest request = new ScanRequest(tableName)
				.withScanFilter(scanFilter);

		ScanResult result = dynamoDBClient.scan(request);

		log.log("Found " + result.getCount() + " results...");
		return result.getItems();
	}

	public Map<String, AttributeValue> getItem(String tableName, Map<String, AttributeValue> key) {
		log.log("Getting item from table " + tableName + " with key " + key + "...");

		GetItemRequest request = new GetItemRequest()
				.withTableName(tableName)
				.withKey(key);

		GetItemResult result = dynamoDBClient.getItem(request);

		if (result.getItem() == null) {
			log.log("No item found...");
			return null;//TODO use optional
		}
		else {
			Map<String, AttributeValue> item = result.getItem();
			log.log("Found item " + item);
			return item;
		}
	}

	// see https://github.com/aws/aws-sdk-java/blob/master/src/samples/AmazonDynamoDB/AmazonDynamoDBSample.java
	//	    // Example: Scan items for movies with a year attribute greater than 1985
	//	    HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
	//	    Condition condition = new Condition()
	//	        .withComparisonOperator(ComparisonOperator.GT.toString())
	//	        .withAttributeValueList(new AttributeValue().withN("1985"));
	//	    scanFilter.put("year", condition);

	//	// just an example of a particular item:
	//	//Map<String, AttributeValue> item = newItem("Bill & Ted's Excellent Adventure", 1989, "****", "James", "Sara");
	//	private static Map<String, AttributeValue> newItem(String name, int year, String rating, String... fans) {
	//		Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
	//		item.put("name", new AttributeValue(name));
	//		item.put("year", new AttributeValue().withN(Integer.toString(year)));
	//		item.put("rating", new AttributeValue(rating));
	//		item.put("fans", new AttributeValue().withSS(fans));
	//
	//		return item;
	//	}

	// enables us to determine the table name based on th ecurrent environment:
	public static class EnvTableNameResolver
	extends DynamoDBMapperConfig.DefaultTableNameResolver {
		@Override
		public String getTableName(Class<?> clazz, DynamoDBMapperConfig config) {
			String base = super.getTableName(clazz, config);
			return base.replaceAll("ENVIRONMENT", envName);
		}
	}

}

