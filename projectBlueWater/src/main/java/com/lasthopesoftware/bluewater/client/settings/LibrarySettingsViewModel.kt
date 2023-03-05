package com.lasthopesoftware.bluewater.client.settings

import android.os.Environment
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.RemoveLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LibrarySettingsViewModel(
	private val libraryProvider: ILibraryProvider,
	private val libraryStorage: ILibraryStorage,
	private val libraryRemoval: RemoveLibraries,
) : ViewModel(), ImmediateResponse<Library?, Unit>, TrackLoadedViewState, ImmediateAction
{
	private var library: Library? = null

	private val mutableIsLoading = MutableStateFlow(false)
	private val mutableIsSaving = MutableStateFlow(false)

	val accessCode = MutableStateFlow("")
	val userName = MutableStateFlow("")
	val password = MutableStateFlow("")
	val customSyncPath = MutableStateFlow("")
	val isLocalOnly = MutableStateFlow(false)
	val syncedFileLocation = MutableStateFlow<Library.SyncedFileLocation?>(null)
	val isWakeOnLanEnabled = MutableStateFlow(false)
	val isUsingExistingFiles = MutableStateFlow(false)
	val isUsingLocalConnectionForSync = MutableStateFlow(false)
	override val isLoading = mutableIsLoading.asStateFlow()
	val isSaving = mutableIsSaving.asStateFlow()

	fun loadLibrary(libraryId: LibraryId): Promise<*> {
		mutableIsLoading.value = true

		Environment.getExternalStorageDirectory()?.path?.also { customSyncPath.value = it }

		return libraryProvider
			.getLibrary(libraryId)
			.then(this)
			.must(this)
	}

	fun saveLibrary(): Promise<Unit> {
		mutableIsSaving.value = true
		val localLibrary = library ?: Library(_nowPlayingId = -1)

		library = localLibrary
			.setAccessCode(accessCode.value)
			.setUserName(userName.value)
			.setPassword(password.value)
			.setLocalOnly(isLocalOnly.value)
			.setCustomSyncedFilesPath(customSyncPath.value)
			.setSyncedFileLocation(syncedFileLocation.value)
			.setIsUsingExistingFiles(isUsingExistingFiles.value)
			.setIsSyncLocalConnectionsOnly(isUsingLocalConnectionForSync.value)
			.setIsWakeOnLanEnabled(isWakeOnLanEnabled.value)

		return libraryStorage
			.saveLibrary(localLibrary)
			.must(this)
			.unitResponse()
	}

	fun removeLibrary(): Promise<*> = library?.let(libraryRemoval::removeLibrary).keepPromise()

	override fun respond(result: Library?) {
		library = result ?: return

		isLocalOnly.value = result.isLocalOnly
		isUsingExistingFiles.value = result.isUsingExistingFiles
		isUsingLocalConnectionForSync.value = result.isSyncLocalConnectionsOnly
		isWakeOnLanEnabled.value = result.isWakeOnLanEnabled

		customSyncPath.value = result.customSyncedFilesPath ?: ""
		syncedFileLocation.value = result.syncedFileLocation

		accessCode.value = result.accessCode ?: ""
		userName.value = result.userName ?: ""
		password.value = result.password ?: ""
	}

	override fun act() {
		mutableIsLoading.value = false
		mutableIsSaving.value = false
	}
}
