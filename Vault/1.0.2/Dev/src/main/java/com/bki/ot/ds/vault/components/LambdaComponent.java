package com.bki.ot.ds.vault.components;

import com.bki.ot.ds.vault.util.MiscUtils;

public abstract class LambdaComponent {

	public static String region = MiscUtils.getDefaultRegion();
	
	// no-arg constructor required
	public LambdaComponent() {}
	
	public abstract void init();
	
}
