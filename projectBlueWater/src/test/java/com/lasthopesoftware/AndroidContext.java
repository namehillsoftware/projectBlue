package com.lasthopesoftware;

import org.junit.runner.RunWith;

@RunWith(AndroidContextRunner.class)
//@LooperMode(LooperMode.Mode.PAUSED)
public abstract class AndroidContext {

	public abstract void before() throws Exception;
}
