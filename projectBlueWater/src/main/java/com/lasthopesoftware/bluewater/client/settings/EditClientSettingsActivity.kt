package com.lasthopesoftware.bluewater.client.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRemoval
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.views.RemoveLibraryConfirmationDialogBuilder
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.permissions.write.ApplicationWritePermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.getValue
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import com.lasthopesoftware.resources.strings.StringResources

class EditClientSettingsActivity :
	AppCompatActivity(),
	View.OnClickListener
{
	companion object {
		val serverIdExtra by lazy { MagicPropertyBuilder.buildMagicPropertyName<EditClientSettingsActivity>("serverIdExtra") }
		private const val permissionsRequestInteger = 1
	}

	private val saveButton by LazyViewFinder<Button>(this, R.id.btnConnect)
	private val applicationWritePermissionsRequirementsProviderLazy by lazy { ApplicationWritePermissionsRequirementsProvider(this) }
	private val applicationReadPermissionsRequirementsProviderLazy by lazy { ApplicationReadPermissionsRequirementsProvider(this) }
	private val libraryProvider by lazy { LibraryRepository(this) }
	private val libraryStorage by lazy {
		ObservableConnectionSettingsLibraryStorage(
            LibraryRepository(this),
            ConnectionSettingsLookup(libraryProvider),
            applicationMessageBus
        )
	}
	private val applicationSettingsRepository by lazy { getApplicationSettingsRepository() }
	private val applicationMessageBus by lazy { getApplicationMessageBus().getScopedMessageBus() }
	private val settingsMenu by lazy {
		EditClientSettingsMenu(
			this,
			StringResources(this),
			RemoveLibraryConfirmationDialogBuilder(
				this,
				LibraryRemoval(
					StoredItemAccess(this),
					libraryStorage,
					getCachedSelectedLibraryIdProvider(),
					libraryProvider,
					BrowserLibrarySelection(applicationSettingsRepository, applicationMessageBus, libraryProvider))))
	}
	private val librarySettingsViewModel by buildViewModelLazily {
		LibrarySettingsViewModel(
			libraryProvider,
			libraryStorage,
			LibraryRemoval(
				StoredItemAccess(this),
				libraryStorage,
				getCachedSelectedLibraryIdProvider(),
				libraryProvider,
				BrowserLibrarySelection(applicationSettingsRepository, applicationMessageBus, libraryProvider))
		)
	}
	private var library: Library? = null

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			ProjectBlueTheme {
				LibrarySettingsView(
					librarySettingsViewModel = librarySettingsViewModel,
					navigateApplication = ActivityApplicationNavigation(this),
					stringResources = StringResources(this)
				)
			}
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean = settingsMenu.buildSettingsMenu(menu)

	override fun onStart() {
		super.onStart()
		initializeLibrary(intent)
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		initializeLibrary(intent)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean =
		settingsMenu.handleSettingsMenuClicks(item, library)

	private fun initializeLibrary(intent: Intent) {
		val libraryId = intent.getIntExtra(serverIdExtra, -1)
		if (libraryId < 0) return

		librarySettingsViewModel.loadLibrary(LibraryId(libraryId))
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		if (requestCode != permissionsRequestInteger) return

		for (grantResult in grantResults) {
			if (grantResult == PackageManager.PERMISSION_GRANTED) continue
			Toast.makeText(this, R.string.permissions_must_be_granted_for_settings, Toast.LENGTH_LONG).show()
			saveButton.isEnabled = true
			return
		}

		saveLibraryAndFinish()
	}

	override fun onClick(v: View?) {
		saveButton.isEnabled = false

		val permissionsToRequest = ArrayList<String>(2)
//		if (applicationReadPermissionsRequirementsProviderLazy.isReadPermissionsRequiredForLibrary(localLibrary))
//			permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
//		if (applicationWritePermissionsRequirementsProviderLazy.isWritePermissionsRequiredForLibrary(localLibrary))
//			permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

		if (permissionsToRequest.size > 0) {
			val permissionsToRequestArray = permissionsToRequest.toTypedArray()
			ActivityCompat.requestPermissions(this, permissionsToRequestArray, permissionsRequestInteger)
		} else {
			saveLibraryAndFinish()
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		applicationMessageBus.close()
	}

	private fun saveLibraryAndFinish() {
		librarySettingsViewModel.saveLibrary().eventually(response({
			saveButton.text = getText(R.string.btn_saved)
			finish()
		}, this))
	}
}
