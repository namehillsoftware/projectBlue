package com.namehillsoftware.projectblue.tv

import android.content.Context
import com.lasthopesoftware.bluewater.ApplicationDependencies
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler

object ApplicationContextAttachedApplicationDependencies {

	private val sync = Any()

	@Volatile
	private var attachedDependencies: AttachedDependencies? = null

	val Context.applicationDependencies: ApplicationDependencies
		// Double-checked initialization
		get() = attachedDependencies
			?.takeIf { it.context == applicationContext }
			?: synchronized(sync) {
				attachedDependencies
					?.takeIf { it.context == applicationContext }
					?: run {
						val newDependencies = AttachedDependencies(applicationContext)
						attachedDependencies = newDependencies
						newDependencies
					}
			}

	private class AttachedDependencies(val context: Context) : ApplicationDependencies {
		override val intentBuilder by lazy { TvIntentBuilder(context) }

		override val syncScheduler by lazy { SyncScheduler(context) }
	}
}
