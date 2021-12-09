package com.lasthopesoftware.bluewater.shared.android.services

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.lasthopesoftware.bluewater.shared.GenericBinder
import com.namehillsoftware.handoff.promises.Promise

inline fun <reified TService : Service> Context.promiseBoundService(): Promise<ConnectedServiceBinding<TService>> {
	val context = this
	return object : Promise<ConnectedServiceBinding<TService>>() {
		init {
			try {
				val c = TService::class.java
				context.bindService(Intent(context, c), object : ServiceConnection {
					override fun onServiceConnected(name: ComponentName?, service: IBinder) {
						val boundService = (service as? GenericBinder<*>)?.service as? TService
						if (boundService == null) {
							reject(InvalidBindingException(c))
							return
						}

						resolve(ConnectedServiceBinding(boundService, this))
					}

					override fun onServiceDisconnected(name: ComponentName?) {}

					override fun onBindingDied(name: ComponentName?) {
						reject(BindingUnexpectedlyDiedException(TService::class.java))
					}

					override fun onNullBinding(name: ComponentName?) {
						reject(UnexpectedNullBindingException(c))
					}
				}, Context.BIND_AUTO_CREATE)
			} catch (err: Throwable) {
				reject(err)
			}
		}
	}
}
