package com.bki.ot.ds.vault.components;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.bki.ot.ds.vault.util.Logger;

public class ComponentsProvider {

	protected Logger log = Logger.LOG;

	protected Map<String, LambdaComponent> activeComponents = new HashMap<>();

	public ComponentsProvider enableComponent(String name, Class<? extends LambdaComponent> type) {
		try {
			// instantiate the component and add to list of active components:
			LambdaComponent component = type.newInstance();
			activeComponents.put(name, component);
		} catch (Exception e) {
			throw new RuntimeException("Failed to instantiate component " + name, e);
		}
		return this;
	}

	public ComponentsProvider init() {
		// a list of init methods that will be invoked concurrently:
		Set<CompletableFuture<?>> initSet = new HashSet<>();
		
		// for every components in the map, launch the init() method concurrently:
		activeComponents.entrySet().forEach(c -> {
			log.log("Initializing component " + c.getKey());
			initSet.add(CompletableFuture.runAsync(() -> c.getValue().init()));
		});
		
		log.log("waiting for all active components to finish initializing...");
		initSet.stream().map(CompletableFuture::join).collect(Collectors.toList());
		log.log("All active components finished initializing...");
		
		return this;
	}

	public <T extends LambdaComponent> T get(String name, Class<T> type) {
		return type.cast(activeComponents.get(name));
	}
	
}
