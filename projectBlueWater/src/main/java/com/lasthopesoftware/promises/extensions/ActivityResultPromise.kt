package com.lasthopesoftware.promises.extensions

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.processors.PublishProcessor

fun ComponentActivity.registerResultActivityLauncher(): LaunchActivitiesForResults = ActivityResultObserver(this)

interface LaunchActivitiesForResults {
	fun promiseResult(intent: Intent): Promise<ActivityResult>
}

private class ActivityResultObserver(activity: ComponentActivity) : ActivityResultCallback<ActivityResult>,
	LaunchActivitiesForResults, LifecycleEventObserver {

	private val lifecycle = activity.lifecycle
	private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult(), this)
	private val publisher by lazy { PublishProcessor.create<ActivityResult>() }

	init {
	    lifecycle.addObserver(this)
	}

	override fun onActivityResult(result: ActivityResult) {
		publisher.offer(result)
	}

	override fun promiseResult(intent: Intent): Promise<ActivityResult> {
		val promisedResult = publisher.toObservable().promiseFirstResult()
		launcher.launch(intent)
		return promisedResult
	}

	override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
		if (source == lifecycle && event.targetState == Lifecycle.State.DESTROYED) {
			launcher.unregister()
			lifecycle.removeObserver(this)
		}
	}
}
