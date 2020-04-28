package com.bki.ot.ls.vault.lambda;

import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.bki.ot.ds.vault.components.ComponentsProvider;
import com.bki.ot.ds.vault.components.DynamoDBComponent;
import com.bki.ot.ds.vault.components.EmailComponent;
import com.bki.ot.ds.vault.components.LambdaComponent;
import com.bki.ot.ds.vault.components.ParamStoreComponent;
import com.bki.ot.ds.vault.components.PresigendPostComponent;
import com.bki.ot.ds.vault.components.S3Component;
import com.bki.ot.ds.vault.components.SQSComponent;
import com.bki.ot.ds.vault.components.STSComponent;

public class ComponentsProviderMock extends ComponentsProvider {

	@Spy
	S3Component s3ComponentMock;

	@Spy
	SQSComponent sqsComponentMock;

	@Spy
	STSComponent stsComponentMock;

	@Spy
	ParamStoreComponent paramStoreComponentMock;

	@Spy
	DynamoDBComponent dynamoMock;

	@Spy
	EmailComponent emailComponentMock;

	@Spy
	PresigendPostComponent presignedPostMock;

	private boolean initialized = false;

	public ComponentsProviderMock() {
		super();
		MockitoAnnotations.initMocks(this);
	}

	@Override
	public ComponentsProvider enableComponent(String name, Class<? extends LambdaComponent> type) {
		try {
			LambdaComponent component = getMock(type);
			activeComponents.put(name, component);
		} catch (Exception e) {
			throw new RuntimeException("Failed to instantiate component " + name, e);
		}
		return this;
	}

	@Override
	public ComponentsProvider init() {
		log.log("Initializing component provider mock...");
		initialized  = true;
		return this;
	}
	
	@Override
	public <T extends LambdaComponent> T get(String name, Class<T> type) {
		if (!initialized) {
			throw new RuntimeException("ComponentsProvider.init() was not called!");			
		}
		//return super.get(name,  type);
		return type.cast(getMock(type));  //---CFH
	}
	
	private LambdaComponent getMock(Class<? extends LambdaComponent> type) {
		if (type.equals(EmailComponent.class)) {
			return emailComponentMock;
		} else if (type.equals(ParamStoreComponent.class)) {
			return paramStoreComponentMock;
		} else if (type.equals(SQSComponent.class)) {
			return sqsComponentMock;
		} else if (type.equals(STSComponent.class)) {
			return stsComponentMock;
		} else if (type.equals(S3Component.class)) {
			return s3ComponentMock;
		} else if (type.equals(PresigendPostComponent.class)) {
			return presignedPostMock;
		} else if (type.equals(DynamoDBComponent.class)) {
			return dynamoMock;
		} else 
			throw new RuntimeException("Unknown mocked type: " + type);
	}
}