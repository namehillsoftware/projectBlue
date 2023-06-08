package com.lasthopesoftware.compilation

import com.lasthopesoftware.bluewater.BuildConfig

object DebugFlag : FlagCompilationForDebugging {
	override val isDebugCompilation = BuildConfig.DEBUG
}
