package com.lasthopesoftware.bluewater.shared.specs;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public abstract class AndroidContext {

	private static final Object syncObject = new Object();
	private static boolean isSetup;

	@Before
	public final void setupAndroidContextOnce() throws Throwable {
		synchronized (syncObject) {
			if (isSetup) return;

			before();
			isSetup = true;
		}
	}

	public abstract void before() throws Throwable;
}
