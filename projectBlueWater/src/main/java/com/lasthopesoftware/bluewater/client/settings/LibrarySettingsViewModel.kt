package com.lasthopesoftware.bluewater.client.settings

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.RemoveLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.permissions.RequestApplicationPermissions
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
	private val applicationPermissions: RequestApplicationPermissions,
) : ViewModel(), PromisedResponse<Boolean, Boolean>, ImmediateResponse<Library?, Unit>, TrackLoadedViewState, ImmediateAction
{
	private var library: Library? = null
	private var modifiedLibrary: Library? = null

	private val mutableIsLoading = MutableStateFlow(false)
	private val mutableIsSaving = MutableStateFlow(false)
	private val mutableIsPermissionsNeeded = MutableStateFlow(false)
	private val mutableIsRemovalRequested = MutableStateFlow(false)
	private val mutableIsSettingsChanged = MutableStateFlow(false)

	val accessCode = MutableStateFlow("")
	val libraryName = MutableStateFlow("")
	val userName = MutableStateFlow("")
	val password = MutableStateFlow("")
	val isLocalOnly = MutableStateFlow(false)
	val syncedFileLocation = MutableStateFlow(Library.SyncedFileLocation.INTERNAL)
	val isWakeOnLanEnabled = MutableStateFlow(false)
	val isUsingExistingFiles = MutableStateFlow(false)
	val isSyncLocalConnectionsOnly = MutableStateFlow(false)

	override val isLoading = mutableIsLoading.asStateFlow()
	val isSaving = mutableIsSaving.asStateFlow()
	val isStoragePermissionsNeeded = mutableIsPermissionsNeeded.asStateFlow()
	val isRemovalRequested = mutableIsRemovalRequested.asStateFlow()
	val isSettingsChanged = mutableIsSettingsChanged.asStateFlow()

	val activeLibraryId
		get() = library?.libraryId

	fun loadLibrary(libraryId: LibraryId): Promise<*> {
		mutableIsLoading.value = true

		return libraryProvider
			.promiseLibrary(libraryId)
			.then(this)
			.must(this)
	}

	fun saveLibrary(): Promise<Boolean> {
		mutableIsSaving.value = true
		val localLibrary = library ?: Library(nowPlayingId = -1)

		library = localLibrary
			.copy(
				accessCode = accessCode.value,
				userName = userName.value,
				password = password.value,
				isLocalOnly = isLocalOnly.value,
				syncedFileLocation = syncedFileLocation.value,
				isUsingExistingFiles = isUsingExistingFiles.value,
				isSyncLocalConnectionsOnly = isSyncLocalConnectionsOnly.value,
				isWakeOnLanEnabled = isWakeOnLanEnabled.value,
				libraryName = libraryName.value
			)

		return applicationPermissions
			.promiseIsLibraryPermissionsGranted(localLibrary)
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

		syncedFileLocation.value = result?.syncedFileLocation ?: Library.SyncedFileLocation.INTERNAL

		accessCode.value = result?.accessCode ?: ""
		userName.value = result?.userName ?: ""
		password.value = result?.password ?: ""
		libraryName.value = result?.libraryName ?: ""
	}

	override fun promiseResponse(resolution: Boolean): Promise<Boolean> {
		val isPermissionsNeeded = !resolution
		mutableIsPermissionsNeeded.value = isPermissionsNeeded

		if (isPermissionsNeeded) return false.toPromise()

		val localLibrary = library ?: return false.toPromise()

		library = localLibrary

		return libraryStorage
			.saveLibrary(localLibrary)
			.then { true }
	}

	override fun act() {
		mutableIsLoading.value = false
		mutableIsSaving.value = false
	}
}
