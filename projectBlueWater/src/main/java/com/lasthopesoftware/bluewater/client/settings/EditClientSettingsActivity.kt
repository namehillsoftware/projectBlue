package com.lasthopesoftware.bluewater.client.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRemoval
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.permissions.write.ApplicationWritePermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.resources.closables.lazyScoped
import com.lasthopesoftware.resources.intents.IntentFactory
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class EditClientSettingsActivity :
	AppCompatActivity(),
	ActivityCompat.OnRequestPermissionsResultCallback,
	ManagePermissions
{
	companion object {
		val serverIdExtra by lazy { MagicPropertyBuilder.buildMagicPropertyName<EditClientSettingsActivity>("serverIdExtra") }
	}

	private val applicationMessageBus by lazyScoped { getApplicationMessageBus().getScopedMessageBus() }
	private val applicationWritePermissionsRequirementsProvider by lazy { ApplicationWritePermissionsRequirementsProvider(this) }
	private val applicationReadPermissionsRequirementsProvider by lazy { ApplicationReadPermissionsRequirementsProvider(this) }
	private val libraryProvider by lazy { LibraryRepository(this) }
	private val libraryStorage by lazy {
		ObservableConnectionSettingsLibraryStorage(
            LibraryRepository(this),
            ConnectionSettingsLookup(libraryProvider),
            applicationMessageBus
        )
	}
	private val applicationSettingsRepository by lazy { getApplicationSettingsRepository() }
	private val librarySettingsViewModel by buildViewModelLazily {
		LibrarySettingsViewModel(
			libraryProvider,
			libraryStorage,
			LibraryRemoval(
				StoredItemAccess(this),
				libraryStorage,
				getCachedSelectedLibraryIdProvider(),
				libraryProvider,
				BrowserLibrarySelection(applicationSettingsRepository, applicationMessageBus, libraryProvider)
			),
			applicationReadPermissionsRequirementsProvider,
			applicationWritePermissionsRequirementsProvider,
			this
		)
	}

	private val permissionsRequestRef = AtomicInteger(0)

	private val permissionsRequests = ConcurrentHashMap<Int, Messenger<Map<String, Boolean>>>()

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			ProjectBlueTheme {
				LibrarySettingsView(
					librarySettingsViewModel = librarySettingsViewModel,
					navigateApplication = ActivityApplicationNavigation(
						this,
						EditClientSettingsActivityIntentBuilder(IntentFactory(this))
					),
					stringResources = StringResources(this)
				)
			}
		}
	}

	override fun onStart() {
		super.onStart()
		initializeLibrary(intent)
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		initializeLibrary(intent)
	}

	private fun initializeLibrary(intent: Intent) {
		val libraryId = intent.getIntExtra(serverIdExtra, -1)
		if (libraryId < 0) return

		librarySettingsViewModel.loadLibrary(LibraryId(libraryId))
	}

	override fun requestPermissions(permissions: List<String>): Promise<Map<String, Boolean>> {
		return if (permissions.isEmpty()) Promise(emptyMap())
		else Promise<Map<String, Boolean>> { messenger ->
			val requestId = permissionsRequestRef.getAndIncrement()
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

		permissionsRequests.remove(requestCode)
			?.sendResolution(grantResults.zip(permissions).associate { (r, p) -> Pair(p, r == PackageManager.PERMISSION_GRANTED) })
	}
}
