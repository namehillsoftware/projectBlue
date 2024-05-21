package com.lasthopesoftware.bluewater.client

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.ActivityDependencies
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.browsing.navigation.NavigationMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingTvApplication
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.permissions.ApplicationPermissionsRequests
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.shared.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.registerResultActivityLauncher
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

private val logger by lazyLogger<TvActivity>()

@UnstableApi
class TvActivity :
	AppCompatActivity(),
	PermissionsDependencies,
	ManagePermissions,
	ActivitySuppliedDependencies {
	private val dependencies by lazy { ActivityDependencies(this, this) }

	override val registeredActivityResultsLauncher = registerResultActivityLauncher()

	override val applicationPermissions by lazy {
		val osPermissionChecker = OsPermissionsChecker(applicationContext)
		ApplicationPermissionsRequests(
			dependencies.libraryProvider,
			ApplicationReadPermissionsRequirementsProvider(osPermissionChecker),
			this,
			osPermissionChecker
		)
	}

	private val permissionsRequests = ConcurrentHashMap<Int, Messenger<Map<String, Boolean>>>()

	override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
		super.onCreate(savedInstanceState, persistentState)

		setContent {
			ProjectBlueTheme {
				NowPlayingTvApplication(
					browserViewDependencies = dependencies,
					permissionsDependencies = this,
					initialDestination = getDestination(intent),
				)
			}
		}
	}

	override fun requestPermissions(permissions: List<String>): Promise<Map<String, Boolean>> = permissions.associateWith { false }.toPromise()

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)

		this.intent = intent

		getDestination(intent)?.also { dependencies.navigationMessages.sendMessage(NavigationMessage(it)) }
	}

	private fun getDestination(intent: Intent?): Destination? =
		intent?.safelyGetParcelableExtra<Destination>(destinationProperty)
}
