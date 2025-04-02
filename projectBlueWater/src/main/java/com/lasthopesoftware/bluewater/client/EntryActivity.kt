package com.lasthopesoftware.bluewater.client

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.lasthopesoftware.bluewater.ActivityDependencies
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies
import com.lasthopesoftware.bluewater.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.browsing.navigation.NavigationMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingTvApplication
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.permissions.ApplicationPermissionsRequests
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker
import com.lasthopesoftware.bluewater.shared.android.ui.ProjectBlueComposableApplication
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.registerResultActivityLauncher
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

private val logger by lazyLogger<EntryActivity>()
private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<EntryActivity>()) }
private val leanbackModeProperty by lazy { magicPropertyBuilder.buildProperty("isInLeanbackMode") }

val destinationProperty by lazy { magicPropertyBuilder.buildProperty("destination") }

class EntryActivity :
	AppCompatActivity(),
	ActivityCompat.OnRequestPermissionsResultCallback,
	ManagePermissions,
	PermissionsDependencies,
	ActivitySuppliedDependencies
{
	private val browserViewDependencies by lazy { ActivityDependencies(this, this, applicationDependencies) }

	override val registeredActivityResultsLauncher = registerResultActivityLauncher()

	override val applicationPermissions by lazy {
		val osPermissionChecker = OsPermissionsChecker(applicationContext)
		ApplicationPermissionsRequests(
			browserViewDependencies.librarySettingsProvider,
			ApplicationReadPermissionsRequirementsProvider(osPermissionChecker),
			this,
			osPermissionChecker
		)
	}

	private val permissionsRequests = ConcurrentHashMap<Int, Messenger<Map<String, Boolean>>>()

	private var isInLeanbackMode = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Ensure that this task is only started when it's the task root. A workaround for an Android bug.
		// See http://stackoverflow.com/a/7748416
		val intent = intent
		if (Intent.ACTION_MAIN == intent.action) {
			if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && !isTaskRoot) {
				val className = javaClass.name
				logger.info("$className is not the root.  Finishing $className instead of launching.")
				finish()
				return
			}

			isInLeanbackMode = intent.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
		}

		applicationPermissions.promiseApplicationPermissionsRequest()

		WindowCompat.setDecorFitsSystemWindows(window, false)

		setContent {
			if (!isInLeanbackMode) {
				ProjectBlueComposableApplication {
					HandheldApplication(
						entryDependencies = browserViewDependencies,
						permissionsDependencies = this,
						initialDestination = getDestination(intent),
					)
				}
			} else {
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
				ProjectBlueComposableApplication(darkTheme = true) {
					NowPlayingTvApplication(
						entryDependencies = browserViewDependencies,
						permissionsDependencies = this,
						initialDestination = getDestination(intent),
					)
				}
			}
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)

		// To preserve state within the Compose application, do not store the new intent.
		val destination = getDestination(intent)
		if (destination != null)
			browserViewDependencies.navigationMessages.sendMessage(NavigationMessage(destination))
	}

	override fun requestPermissions(permissions: List<String>): Promise<Map<String, Boolean>> {
		return if (permissions.isEmpty()) Promise(emptyMap())
		else Promise<Map<String, Boolean>> { messenger ->
			val requestId = messenger.hashCode()
			permissionsRequests[requestId] = messenger

			ActivityCompat.requestPermissions(
				this,
				permissions.toTypedArray(),
				requestId
			)
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		outState.putBoolean(leanbackModeProperty, isInLeanbackMode)

		super.onSaveInstanceState(outState)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)

		isInLeanbackMode = savedInstanceState.getBoolean(leanbackModeProperty)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		permissionsRequests
			.remove(requestCode)
			?.sendResolution(
				grantResults
					.zip(permissions)
					.associate { (r, p) -> Pair(p, r == PackageManager.PERMISSION_GRANTED) }
			)
	}

	private fun getDestination(intent: Intent?): Destination? =
		intent?.safelyGetParcelableExtra<Destination>(destinationProperty)
}
