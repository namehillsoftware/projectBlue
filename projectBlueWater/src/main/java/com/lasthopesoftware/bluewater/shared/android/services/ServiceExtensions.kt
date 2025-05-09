package com.lasthopesoftware.bluewater.shared.android.services

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.cancellation.CancellationToken
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.Logger
import kotlin.coroutines.cancellation.CancellationException

interface BoundService<TService : Service> : AutoCloseable {
	val service: TService
}

class ConnectedServiceBinding<TService : Service>(
	private val owningContext: Context,
	private val serviceConnection: ServiceConnection,
	override val service: TService
) : BoundService<TService> {
	companion object {
		private val logger: Logger by lazyLogger<BoundService<*>>()
	}

	override fun close() {
		try {
			owningContext.unbindService(serviceConnection)
		} catch (e: Exception) {
			logger.error("There was an error unbinding service ${service.javaClass.canonicalName}", e)
		}
	}
}

inline fun <reified TService : Service> Context.promiseBoundService(extras: Bundle? = null): Promise<BoundService<TService>> =
	object : Promise<BoundService<TService>>(), ServiceConnection {

		private val serviceClass = cls<TService>()

		// Only look for a cancellation signal - once the service is bound, it needs to be closed.
		private val cancellationToken = CancellationToken()

		init {
			awaitCancellation(cancellationToken)

			try {
				val intent = Intent(this@promiseBoundService, serviceClass).apply {
					replaceExtras(extras)
				}

				if (!cancellationToken.isCancelled)
					bindService(intent, this, Context.BIND_AUTO_CREATE)
			} catch (err: Throwable) {
				reject(err)
			}
		}

		override fun onServiceConnected(name: ComponentName?, service: IBinder) {
			try {
				if (cancellationToken.isCancelled) {
					unbindService(this)
					reject(CancellationException("Service Binding cancelled"))
					return
				}

				val boundService = (service as? GenericBinder<*>)?.service as? TService
				if (boundService == null) {
					unbindService(this)
					reject(InvalidBindingException(serviceClass))
					return
				}

				resolve(ConnectedServiceBinding(this@promiseBoundService, this, boundService))
			} catch (e: Throwable) {
				reject(e)
			}
		}

		override fun onServiceDisconnected(name: ComponentName?) {}

		override fun onBindingDied(name: ComponentName?) {
			reject(BindingUnexpectedlyDiedException(serviceClass))
		}

		override fun onNullBinding(name: ComponentName?) {
			reject(UnexpectedNullBindingException(serviceClass))
		}
	}
