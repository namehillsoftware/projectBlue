package com.lasthopesoftware.compilation

import com.lasthopesoftware.bluewater.BuildConfig

object DebugFlag : FlagCompilationForDebugging {
	override fun isDebugCompilation(): Boolean = BuildConfig.DEBUG
}
