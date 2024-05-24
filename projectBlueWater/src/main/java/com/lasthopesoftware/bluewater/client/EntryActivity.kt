package com.lasthopesoftware.bluewater.client

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.ActivityDependencies
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.browsing.navigation.NavigationMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingTvApplication
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.permissions.ApplicationPermissionsRequests
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.registerResultActivityLauncher
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

private val logger by lazyLogger<EntryActivity>()
private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<EntryActivity>()) }

val destinationProperty by lazy { magicPropertyBuilder.buildProperty("destination") }

@UnstableApi class EntryActivity :
	AppCompatActivity(),
	ActivityCompat.OnRequestPermissionsResultCallback,
	ManagePermissions,
	PermissionsDependencies,
	ActivitySuppliedDependencies
{
	private val browserViewDependencies by lazy { ActivityDependencies(this, this) }

	override val registeredActivityResultsLauncher = registerResultActivityLauncher()

	override val applicationPermissions by lazy {
		val osPermissionChecker = OsPermissionsChecker(applicationContext)
		ApplicationPermissionsRequests(
			browserViewDependencies.libraryProvider,
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
			ProjectBlueTheme {
				if (!isInLeanbackMode) {
					NarrowScreenApplication(
						browserViewDependencies = browserViewDependencies,
						permissionsDependencies = this,
						initialDestination = getDestination(intent),
					)
				} else {
					NowPlayingTvApplication(
						browserViewDependencies = browserViewDependencies,
						permissionsDependencies = this,
						initialDestination = getDestination(intent),
					)
				}
			}
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)

		this.intent = intent

		getDestination(intent)?.also { browserViewDependencies.navigationMessages.sendMessage(NavigationMessage(it)) }
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
