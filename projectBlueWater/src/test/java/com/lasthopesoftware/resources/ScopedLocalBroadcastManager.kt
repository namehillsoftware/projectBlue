package com.lasthopesoftware.resources

import android.content.Context
import android.os.Handler
import androidx.localbroadcastmanager.content.LocalBroadcastManager

object ScopedLocalBroadcastManager {
	private val lazyConstructor = lazy {
		val broadcastManagerConstructor = LocalBroadcastManager::class.java.getDeclaredConstructor(
			Context::class.java
		)
		broadcastManagerConstructor.isAccessible = true
		broadcastManagerConstructor
	}

	@JvmStatic
	fun newScopedBroadcastManager(context: Context): LocalBroadcastManager = lazyConstructor.value.newInstance(context)

	@JvmStatic
	fun processMessages(context: Context) {
		val handler = Handler(context.mainLooper)
		handler.sendEmptyMessage(1)
	}
}
