package com.lasthopesoftware.bluewater.shared.android.services

import android.content.Context
import android.content.ServiceConnection
import com.lasthopesoftware.bluewater.shared.lazyLogger

private val logger by lazyLogger<ConnectedServiceBinding<*>>()

class ConnectedServiceBinding<TService : Any>(private val owningContext: Context, private val serviceConnection: ServiceConnection, val service: TService) : AutoCloseable {
	override fun close() {
		try {
			owningContext.unbindService(serviceConnection)
		} catch (e: Exception) {
			logger.error("There was an error unbinding service ${service.javaClass.canonicalName}", e)
		}
	}
}
