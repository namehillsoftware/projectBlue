package com.lasthopesoftware.compilation;

import com.lasthopesoftware.bluewater.BuildConfig;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

public class DebugFlag implements FlagCompilationForDebugging {

	private DebugFlag() {}

	@Override
	public boolean isDebugCompilation() {
		return BuildConfig.DEBUG;
	}

	private static final CreateAndHold<DebugFlag> lazyInstance = new Lazy<>(DebugFlag::new);

	public static DebugFlag getInstance() {
		return lazyInstance.getObject();
	}
}
