package com.lasthopesoftware.bluewater.shared.promises.extensions

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.namehillsoftware.handoff.promises.Promise

fun ComponentActivity.promiseActivityResult(intent: Intent) = ActivityResultPromise(this, intent)

class ActivityResultPromise(activity: ComponentActivity, intent: Intent) : Promise<ActivityResult?>(),
	ActivityResultCallback<ActivityResult> {
	init {
		val launcher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult(), this)
		launcher.launch(intent)
	}

	override fun onActivityResult(result: ActivityResult) {
		resolve(result)
	}
}
