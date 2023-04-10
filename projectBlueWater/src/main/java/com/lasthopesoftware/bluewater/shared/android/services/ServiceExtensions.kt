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
	object : Promise<ConnectedServiceBinding<TService>>() {
		init {
			try {
				val c = cls<TService>()
				val intent = Intent(this@promiseBoundService, c).apply {
					replaceExtras(extras)
				}

				bindService(intent, object : ServiceConnection {
					override fun onServiceConnected(name: ComponentName?, service: IBinder) {
						val boundService = (service as? GenericBinder<*>)?.service as? TService
						if (boundService != null) {
							resolve(ConnectedServiceBinding(boundService, this))
							return
						}

						unbindService(this)
						reject(InvalidBindingException(c))
					}

					override fun onServiceDisconnected(name: ComponentName?) {}

					override fun onBindingDied(name: ComponentName?) {
						reject(BindingUnexpectedlyDiedException(c))
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
