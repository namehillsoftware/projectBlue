package com.lasthopesoftware.bluewater

import android.content.Context
import com.lasthopesoftware.bluewater.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.client.ActivitySuppliedDependencies
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler

object ApplicationDependenciesContainer {

	private val sync = Any()

	@Volatile
	private var attachedDependencies: AttachedDependencies? = null

	val Context.applicationDependencies: ApplicationDependencies
		// Double-checked initialization
		get() = attachedDependencies
			?.takeIf { it.applicationContext == applicationContext }
			?: synchronized(sync) {
				attachedDependencies
					?.takeIf { it.applicationContext == applicationContext }
					?: run {
						val newDependencies = AttachedDependencies(applicationContext, this)
						attachedDependencies = newDependencies
						newDependencies
					}
			}

	fun refresh() = synchronized(sync) {
		attachedDependencies = null
	}

	private class AttachedDependencies(val applicationContext: Context, private val callingContext: Context) : ApplicationDependencies {
		override val intentBuilder: BuildIntents by lazy {
			if (callingContext is ActivitySuppliedDependencies) callingContext.intentBuilder
			else throw UnsupportedOperationException("$callingContext could not supply an intentBuilder")
		}

		override val syncScheduler by lazy { SyncScheduler(applicationContext) }
	}
}
