package com.lasthopesoftware.bluewater.client.settings

import android.Manifest
import android.os.Environment
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.RemoveLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.permissions.read.ProvideReadPermissionsRequirements
import com.lasthopesoftware.bluewater.permissions.write.ProvideWritePermissionsRequirements
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LibrarySettingsViewModel(
	private val libraryProvider: ILibraryProvider,
	private val libraryStorage: ILibraryStorage,
	private val libraryRemoval: RemoveLibraries,
	private val applicationReadPermissionsRequirementsProvider: ProvideReadPermissionsRequirements,
	private val applicationWritePermissionsRequirementsProvider: ProvideWritePermissionsRequirements,
	private val permissionsManager: ManagePermissions,
) : ViewModel(), PromisedResponse<Map<String, Boolean>, Boolean>, ImmediateResponse<Library?, Unit>, TrackLoadedViewState, ImmediateAction
{
	private var library: Library? = null

	private val mutableIsLoading = MutableStateFlow(false)
	private val mutableIsSaving = MutableStateFlow(false)
	private val mutableIsPermissionsNeeded = MutableStateFlow(false)
	private val mutableIsRemovalRequested = MutableStateFlow(false)

	val accessCode = MutableStateFlow("")
	val userName = MutableStateFlow("")
	val password = MutableStateFlow("")
	val customSyncPath = MutableStateFlow(Environment.getExternalStorageDirectory()?.path ?: "")
	val isLocalOnly = MutableStateFlow(false)
	val syncedFileLocation = MutableStateFlow(Library.SyncedFileLocation.INTERNAL)
	val isWakeOnLanEnabled = MutableStateFlow(false)
	val isUsingExistingFiles = MutableStateFlow(false)
	val isSyncLocalConnectionsOnly = MutableStateFlow(false)
	override val isLoading = mutableIsLoading.asStateFlow()
	val isSaving = mutableIsSaving.asStateFlow()
	val isPermissionsNeeded = mutableIsPermissionsNeeded.asStateFlow()
	val isRemovalRequested = mutableIsRemovalRequested.asStateFlow()

	fun loadLibrary(libraryId: LibraryId): Promise<*> {
		mutableIsLoading.value = true

		return libraryProvider
			.getLibrary(libraryId)
			.then(this)
			.must(this)
	}

	fun saveLibrary(): Promise<Boolean> {
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
			.setIsSyncLocalConnectionsOnly(isSyncLocalConnectionsOnly.value)
			.setIsWakeOnLanEnabled(isWakeOnLanEnabled.value)

		val permissionsToRequest = ArrayList<String>(2)
		if (applicationReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(localLibrary))
			permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
		if (applicationWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(localLibrary))
			permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

		return permissionsManager
			.requestPermissions(permissionsToRequest)
			.eventually(this)
			.must(this)
	}

	fun requestLibraryRemoval() {
		mutableIsRemovalRequested.value = true
	}

	fun cancelLibraryRemovalRequest() {
		mutableIsRemovalRequested.value = false
	}

	fun removeLibrary(): Promise<*> = library?.takeIf { mutableIsRemovalRequested.value }?.let(libraryRemoval::removeLibrary).keepPromise()

	override fun respond(result: Library?) {
		library = result

		isLocalOnly.value = result?.isLocalOnly ?: false
		isUsingExistingFiles.value = result?.isUsingExistingFiles ?: false
		isSyncLocalConnectionsOnly.value = result?.isSyncLocalConnectionsOnly ?: false
		isWakeOnLanEnabled.value = result?.isWakeOnLanEnabled ?: false

		customSyncPath.value = result?.customSyncedFilesPath?.takeIf { it.isNotEmpty() } ?: Environment.getExternalStorageDirectory()?.path ?: ""
		syncedFileLocation.value = result?.syncedFileLocation ?: Library.SyncedFileLocation.INTERNAL

		accessCode.value = result?.accessCode ?: ""
		userName.value = result?.userName ?: ""
		password.value = result?.password ?: ""
	}

	override fun promiseResponse(resolution: Map<String, Boolean>): Promise<Boolean> {
		val isPermissionsNeeded = resolution.values.any { !it }
		mutableIsPermissionsNeeded.value = isPermissionsNeeded

		if (isPermissionsNeeded) return false.toPromise()

		val localLibrary = library ?: return false.toPromise()

		library = localLibrary

		return libraryStorage
			.saveLibrary(localLibrary)
			.then { l -> l != null }
	}

	override fun act() {
		mutableIsLoading.value = false
		mutableIsSaving.value = false
	}
}
