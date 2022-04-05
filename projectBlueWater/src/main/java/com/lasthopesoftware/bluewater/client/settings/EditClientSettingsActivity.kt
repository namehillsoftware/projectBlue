package com.lasthopesoftware.bluewater.client.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRemoval
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.views.RemoveLibraryConfirmationDialogBuilder
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.permissions.write.ApplicationWritePermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class EditClientSettingsActivity :
	AppCompatActivity(),
	View.OnClickListener,
	RadioGroup.OnCheckedChangeListener,
	ImmediateResponse<Library?, Unit>
{

	companion object {
		val serverIdExtra by lazy { MagicPropertyBuilder.buildMagicPropertyName<EditClientSettingsActivity>("serverIdExtra") }
		private const val permissionsRequestInteger = 1
	}

	private val saveButton = LazyViewFinder<Button>(this, R.id.btnConnect)
	private val txtAccessCode = LazyViewFinder<EditText>(this, R.id.txtAccessCode)
	private val txtUserName = LazyViewFinder<EditText>(this, R.id.txtUserName)
	private val txtPassword = LazyViewFinder<EditText>(this, R.id.txtPassword)
	private val txtSyncPath = LazyViewFinder<TextView>(this, R.id.txtSyncPath)
	private val chkLocalOnly = LazyViewFinder<CheckBox>(this, R.id.chkLocalOnly)
	private val rgSyncFileOptions = LazyViewFinder<RadioGroup>(this, R.id.rgSyncFileOptions)
	private val chkIsUsingExistingFiles = LazyViewFinder<CheckBox>(this, R.id.chkIsUsingExistingFiles)
	private val chkIsUsingLocalConnectionForSync = LazyViewFinder<CheckBox>(this, R.id.chkIsUsingLocalConnectionForSync)
	private val chkIsWakeOnLanEnabled = LazyViewFinder<CheckBox>(this, R.id.isWakeOnLan)
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
					SelectedBrowserLibraryIdentifierProvider(getApplicationSettingsRepository()),
					libraryProvider,
					BrowserLibrarySelection(applicationSettingsRepository, applicationMessageBus, libraryProvider))))
	}
	private var library: Library? = null

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_edit_server_settings)
		setSupportActionBar(findViewById(R.id.serverSettingsToolbar))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		saveButton.findView().setOnClickListener(this)
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
		with (rgSyncFileOptions.findView()) {
			check(R.id.rbPrivateToApp)
			setOnCheckedChangeListener(this@EditClientSettingsActivity)
		}

		Environment.getExternalStorageDirectory()?.path?.also(txtSyncPath.findView()::setText)

		val libraryId = intent.getIntExtra(serverIdExtra, -1)
		if (libraryId < 0) return

		libraryProvider
			.getLibrary(LibraryId(libraryId))
			.eventually(response(this, this))
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		if (requestCode != permissionsRequestInteger) return

		for (grantResult in grantResults) {
			if (grantResult == PackageManager.PERMISSION_GRANTED) continue
			Toast.makeText(this, R.string.permissions_must_be_granted_for_settings, Toast.LENGTH_LONG).show()
			saveButton.findView().isEnabled = true
			return
		}

		saveLibraryAndFinish()
	}

	override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
		txtSyncPath.findView().isEnabled = checkedId == R.id.rbCustomLocation
	}

	override fun onClick(v: View?) {
		saveButton.findView().isEnabled = false

		val localLibrary = library ?: Library(_nowPlayingId = -1)

		library = localLibrary
			.setAccessCode(txtAccessCode.findView().text.toString())
			.setUserName(txtUserName.findView().text.toString())
			.setPassword(txtPassword.findView().text.toString())
			.setLocalOnly(chkLocalOnly.findView().isChecked)
			.setCustomSyncedFilesPath(txtSyncPath.findView().text.toString())
			.setSyncedFileLocation(when (rgSyncFileOptions.findView().checkedRadioButtonId) {
				R.id.rbPublicLocation -> SyncedFileLocation.EXTERNAL
				R.id.rbPrivateToApp -> SyncedFileLocation.INTERNAL
				R.id.rbCustomLocation -> SyncedFileLocation.CUSTOM
				else -> null
			})
			.setIsUsingExistingFiles(chkIsUsingExistingFiles.findView().isChecked)
			.setIsSyncLocalConnectionsOnly(chkIsUsingLocalConnectionForSync.findView().isChecked)
			.setIsWakeOnLanEnabled(chkIsWakeOnLanEnabled.findView().isChecked)

		val permissionsToRequest = ArrayList<String>(2)
		if (applicationReadPermissionsRequirementsProviderLazy.isReadPermissionsRequiredForLibrary(localLibrary))
			permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
		if (applicationWritePermissionsRequirementsProviderLazy.isWritePermissionsRequiredForLibrary(localLibrary))
			permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
		if (permissionsToRequest.size > 0) {
			val permissionsToRequestArray = permissionsToRequest.toTypedArray()
			ActivityCompat.requestPermissions(this, permissionsToRequestArray, permissionsRequestInteger)
		} else {
			saveLibraryAndFinish()
		}
	}

	override fun respond(result: Library?) {
		library = result ?: return

		chkLocalOnly.findView().isChecked = result.isLocalOnly
		chkIsUsingExistingFiles.findView().isChecked = result.isUsingExistingFiles
		chkIsUsingLocalConnectionForSync.findView().isChecked = result.isSyncLocalConnectionsOnly
		chkIsWakeOnLanEnabled.findView().isChecked = result.isWakeOnLanEnabled

		val customSyncPath = result.customSyncedFilesPath
		if (customSyncPath != null && customSyncPath.isNotEmpty()) txtSyncPath.findView().text = customSyncPath

		rgSyncFileOptions.findView().check(when (result.syncedFileLocation) {
			SyncedFileLocation.EXTERNAL -> R.id.rbPublicLocation
			SyncedFileLocation.INTERNAL -> R.id.rbPrivateToApp
			SyncedFileLocation.CUSTOM -> R.id.rbCustomLocation
			else -> -1
		})

		txtAccessCode.findView().setText(result.accessCode)
		txtUserName.findView().setText(result.userName)
		txtPassword.findView().setText(result.password)
	}

	override fun onDestroy() {
		super.onDestroy()
		applicationMessageBus.close()
	}

	private fun saveLibraryAndFinish() {
		val library = library ?: return

		libraryStorage.saveLibrary(library).eventually(response({
			saveButton.findView().text = getText(R.string.btn_saved)
			finish()
		}, this))
	}
}
