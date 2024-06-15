package com.lasthopesoftware.bluewater

import android.content.Context
import com.lasthopesoftware.bluewater.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler

object AttachedApplicationDependencies {

	private lateinit var attachedDependencies: AttachedDependencies

	fun attach(context: Context, intentBuilder: BuildIntents) {
		attachedDependencies = AttachedDependencies(context.applicationContext, intentBuilder)
	}

	val applicationDependencies: ApplicationDependencies
		get() = attachedDependencies

	private class AttachedDependencies(context: Context, override val intentBuilder: BuildIntents) : ApplicationDependencies {
		override val syncScheduler by lazy { SyncScheduler(context) }
	}
}
