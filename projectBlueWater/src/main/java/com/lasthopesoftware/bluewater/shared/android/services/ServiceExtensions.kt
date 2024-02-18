package com.lasthopesoftware.bluewater.shared.android.services

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.lasthopesoftware.bluewater.shared.cls
import com.namehillsoftware.handoff.promises.Promise

inline fun <reified TService : Service> Context.promiseBoundService(extras: Bundle? = null): Promise<ConnectedServiceBinding<TService>> =
	object : Promise<ConnectedServiceBinding<TService>>(), ServiceConnection {

		private val serviceClass = cls<TService>()

		init {
			try {
				val intent = Intent(this@promiseBoundService, serviceClass).apply {
					replaceExtras(extras)
				}

				bindService(intent, this, Context.BIND_AUTO_CREATE)
			} catch (err: Throwable) {
				reject(err)
			}
		}

		override fun onServiceConnected(name: ComponentName?, service: IBinder) {
			val boundService = (service as? GenericBinder<*>)?.service as? TService
			if (boundService != null) {
				resolve(ConnectedServiceBinding(this@promiseBoundService, this, boundService))
				return
			}

			unbindService(this)
			reject(InvalidBindingException(serviceClass))
		}

		override fun onServiceDisconnected(name: ComponentName?) {}

		override fun onBindingDied(name: ComponentName?) {
			reject(BindingUnexpectedlyDiedException(serviceClass))
		}

		override fun onNullBinding(name: ComponentName?) {
			reject(UnexpectedNullBindingException(serviceClass))
		}
	}
