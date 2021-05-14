package com.lasthopesoftware;

import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@RunWith(AndroidContextRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public abstract class AndroidContext {

	public abstract void before() throws Exception;
}
