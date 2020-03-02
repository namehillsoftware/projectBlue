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
import com.lasthopesoftware.bluewater.about.AboutTitleBuilder
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivity
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.permissions.read.IApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.permissions.write.ApplicationWritePermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.permissions.write.IApplicationWritePermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.settings.SettingsMenu
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.response.ResponseAction
import com.namehillsoftware.handoff.promises.response.VoidResponse
import com.namehillsoftware.lazyj.Lazy
import java.util.*
import java.util.concurrent.Callable

class EditClientSettingsActivity : AppCompatActivity() {
	private val saveButton = LazyViewFinder<Button>(this, R.id.btnConnect)
	private val txtAccessCode = LazyViewFinder<EditText>(this, R.id.txtAccessCode)
	private val txtUserName = LazyViewFinder<EditText>(this, R.id.txtUserName)
	private val txtPassword = LazyViewFinder<EditText>(this, R.id.txtPassword)
	private val txtSyncPath = LazyViewFinder<EditText>(this, R.id.txtSyncPath)
	private val chkLocalOnly = LazyViewFinder<CheckBox>(this, R.id.chkLocalOnly)
	private val rgSyncFileOptions = LazyViewFinder<RadioGroup>(this, R.id.rgSyncFileOptions)
	private val chkIsUsingExistingFiles = LazyViewFinder<CheckBox>(this, R.id.chkIsUsingExistingFiles)
	private val chkIsUsingLocalConnectionForSync = LazyViewFinder<CheckBox>(this, R.id.chkIsUsingLocalConnectionForSync)
	private val chkIsWakeOnLanEnabled = LazyViewFinder<CheckBox>(this, R.id.isWakeOnLan);
	private val applicationWritePermissionsRequirementsProviderLazy = Lazy(Callable<IApplicationWritePermissionsRequirementsProvider> { ApplicationWritePermissionsRequirementsProvider(this) })
	private val applicationReadPermissionsRequirementsProviderLazy = Lazy(Callable<IApplicationReadPermissionsRequirementsProvider> { ApplicationReadPermissionsRequirementsProvider(this) })
	private val lazyLibraryProvider = Lazy(Callable { LibraryRepository(this@EditClientSettingsActivity) })
	private val settingsMenu = SettingsMenu(this, AboutTitleBuilder(this))
	private var library: Library? = null

	private val connectionButtonListener = View.OnClickListener {
		saveButton.findView().isEnabled = false

		if (library == null) {
			library = Library()
			library!!.nowPlayingId = -1
		}

		library!!.accessCode = txtAccessCode.findView().text.toString()
		library!!.userName = txtUserName.findView().text.toString()
		library!!.password = txtPassword.findView().text.toString()
		library!!.isLocalOnly = chkLocalOnly.findView().isChecked
		library!!.customSyncedFilesPath = txtSyncPath.findView().text.toString()

		when (rgSyncFileOptions.findView().checkedRadioButtonId) {
			R.id.rbPublicLocation -> library!!.syncedFileLocation = SyncedFileLocation.EXTERNAL
			R.id.rbPrivateToApp -> library!!.syncedFileLocation = SyncedFileLocation.INTERNAL
			R.id.rbCustomLocation -> library!!.syncedFileLocation = SyncedFileLocation.CUSTOM
		}

		library!!.setIsUsingExistingFiles(chkIsUsingExistingFiles.findView().isChecked)
		library!!.setIsSyncLocalConnectionsOnly(chkIsUsingLocalConnectionForSync.findView().isChecked)
		library!!.setIsWakeOnLanEnabled(chkIsWakeOnLanEnabled.findView().isChecked)

		val permissionsToRequest = ArrayList<String>(2)
		if (applicationReadPermissionsRequirementsProviderLazy.getObject().isReadPermissionsRequiredForLibrary(library!!)) permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
		if (applicationWritePermissionsRequirementsProviderLazy.getObject().isWritePermissionsRequiredForLibrary(library!!)) permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
		if (permissionsToRequest.size > 0) {
			val permissionsToRequestArray = permissionsToRequest.toTypedArray()
			ActivityCompat.requestPermissions(this@EditClientSettingsActivity, permissionsToRequestArray, permissionsRequestInteger)
		} else {
			saveLibraryAndFinish()
		}
	}

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_edit_server_settings)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		saveButton.findView().setOnClickListener(connectionButtonListener)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		return settingsMenu.buildSettingsMenu(menu)
	}

	override fun onStart() {
		super.onStart()
		initializeLibrary(intent)
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		initializeLibrary(intent)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode != selectDirectoryResultId) {
			super.onActivityResult(requestCode, resultCode, data)
			return
		}
		val uri = data?.dataString
		if (uri != null) txtSyncPath.findView().setText(uri)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return settingsMenu.handleSettingsMenuClicks(item)
	}

	private fun initializeLibrary(intent: Intent) {
		val externalFilesDir = Environment.getExternalStorageDirectory()
		val syncPathTextView: TextView = txtSyncPath.findView()
		if (externalFilesDir != null) syncPathTextView.text = externalFilesDir.path

		val syncFilesRadioGroup = rgSyncFileOptions.findView()
		syncFilesRadioGroup.check(R.id.rbPrivateToApp)
		syncFilesRadioGroup.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int -> syncPathTextView.isEnabled = checkedId == R.id.rbCustomLocation }

		val libraryId = intent.getIntExtra(serverIdExtra, -1)
		if (libraryId < 0) return

		lazyLibraryProvider.getObject()
			.getLibrary(LibraryId(libraryId))
			.eventually(LoopedInPromise.response(VoidResponse(ResponseAction<Library> { result ->
				if (result == null) return@ResponseAction

				library = result
				chkLocalOnly.findView().isChecked = result.isLocalOnly
				chkIsUsingExistingFiles.findView().isChecked = result.isUsingExistingFiles
				chkIsUsingLocalConnectionForSync.findView().isChecked = result.isSyncLocalConnectionsOnly
				chkIsWakeOnLanEnabled.findView().isChecked = result.isWakeOnLanEnabled
				val customSyncPath = result.customSyncedFilesPath
				if (customSyncPath != null && !customSyncPath.isEmpty()) syncPathTextView.text = customSyncPath

				when (result.syncedFileLocation) {
					SyncedFileLocation.EXTERNAL -> syncFilesRadioGroup.check(R.id.rbPublicLocation)
					SyncedFileLocation.INTERNAL -> syncFilesRadioGroup.check(R.id.rbPrivateToApp)
					SyncedFileLocation.CUSTOM -> syncFilesRadioGroup.check(R.id.rbCustomLocation)
					null -> syncFilesRadioGroup.clearCheck()
				}

				txtAccessCode.findView().setText(result.accessCode)
				txtUserName.findView().setText(result.userName)
				txtPassword.findView().setText(result.password)
			}), this))
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		if (requestCode != permissionsRequestInteger) return

		for (grantResult in grantResults) {
			if (grantResult == PackageManager.PERMISSION_GRANTED) continue
			Toast.makeText(this, R.string.permissions_must_be_granted_for_settings, Toast.LENGTH_LONG).show()
			saveButton.findView().isEnabled = true
			return
		}

		saveLibraryAndFinish()
	}

	private fun saveLibraryAndFinish() {
		lazyLibraryProvider.getObject().saveLibrary(library).eventually(LoopedInPromise.response<Library, Any?>({
			saveButton.findView().text = getText(R.string.btn_saved)
			finish()
			null
		}, this))
	}

	companion object {
		@JvmField
		val serverIdExtra = EditClientSettingsActivity::class.java.canonicalName + ".serverIdExtra"
		private const val selectDirectoryResultId = 93
		private const val permissionsRequestInteger = 1
	}
}
