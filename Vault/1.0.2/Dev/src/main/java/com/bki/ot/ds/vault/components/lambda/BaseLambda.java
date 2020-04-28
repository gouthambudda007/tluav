package com.bki.ot.ds.vault.components.lambda;

import java.util.concurrent.TimeUnit;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.bki.ot.ds.vault.components.ComponentsProvider;
import com.bki.ot.ds.vault.util.Logger;
import com.bki.ot.ds.vault.util.MiscUtils;

public abstract class BaseLambda<I, O> implements RequestHandler<I, O> {

	protected Logger log = Logger.LOG;

	protected String region;
	protected ComponentsProvider componentsProvider;

	// do all initialization in constructor, to take advantage of 'execution context reuse':
	public BaseLambda() {
		log.log("---------------------- Lambda initializing...");
		
		try {
			initLambda();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		log.log("---------------------- Lambda initializing done...");
	}

	// can be overridden / augmented by concrete class
	protected void initLambda() throws Exception {
		region = MiscUtils.getDefaultRegion();
		componentsProvider = getComponentProvider();
	}

	// can be overridden for unit testing for injecting mock components:
	protected ComponentsProvider getComponentProvider() {
		return new ComponentsProvider();
	}
	
	//	//@Override
	//	public String handleScheduledRequest(ScheduledEvent event, Context context)  {
	//
	//		log.init(context);
	//		log.log("---------------------- Starting handleRequest()...");
	//		//log.prettyPrint("Event", event);
	//
	//		try {
	//			doSoemthing(event);
	//			return "Done";
	//
	//		} catch (BadRequestException e) {
	//			log.log("-------- Completed operation with error(s) due to bad request");
	//			throw e;//TODO
	//
	//		} catch (Exception e) {
	//			log.error("failure detected while handling request", e);
	//			log.log("-------- Completed operation with error(s)");
	//			throw new RuntimeException(e); // TODO
	//		}
	//	}
	// utils: -----------------------------------------------------------------

	protected void sleepSeconds(int delay) {
		try {
			TimeUnit.SECONDS.sleep(delay);
		} catch (InterruptedException e) {
			log.error("Sleep interrupted", e);
		}
	}

}
