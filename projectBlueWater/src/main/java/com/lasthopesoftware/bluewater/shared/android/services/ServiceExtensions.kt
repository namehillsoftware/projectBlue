package com.lasthopesoftware.bluewater.shared.android.services

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.lasthopesoftware.bluewater.client.playback.service.BindingUnexpectedlyDiedException
import com.lasthopesoftware.bluewater.shared.GenericBinder
import com.namehillsoftware.handoff.promises.Promise

inline fun <reified TService : Service> Context.promiseBoundService(): Promise<ConnectedServiceBinding<TService>> {
	val context = this
	return object : Promise<ConnectedServiceBinding<TService>>() {
		init {
			try {
				context.bindService(Intent(context, TService::class.java), object : ServiceConnection {
					override fun onServiceConnected(name: ComponentName?, service: IBinder) {
						resolve(
							ConnectedServiceBinding(
								(service as? GenericBinder<*>)?.service as? TService,
								this
							)
						)
					}

					override fun onServiceDisconnected(name: ComponentName?) {}

					override fun onBindingDied(name: ComponentName?) {
						reject(BindingUnexpectedlyDiedException(TService::class.java))
					}

					override fun onNullBinding(name: ComponentName?) {
						resolve(
							ConnectedServiceBinding(
								null,
								this
							)
						)
					}
				}, Context.BIND_AUTO_CREATE)
			} catch (err: Throwable) {
				reject(err)
			}
		}
	}
}
