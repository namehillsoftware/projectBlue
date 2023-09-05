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
import com.lasthopesoftware.bluewater.shared.observables.MutableStateObservable
import com.lasthopesoftware.bluewater.shared.observables.ReadOnlyStateObservable
import com.lasthopesoftware.bluewater.shared.observables.SingleStateObservable
import com.lasthopesoftware.bluewater.shared.observables.SubscribedStateObservable
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import io.reactivex.Observable
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

	private val mutableIsLoading = MutableStateFlow(false)
	private val mutableIsSaving = MutableStateFlow(false)
	private val mutableIsPermissionsNeeded = MutableStateFlow(false)
	private val mutableIsRemovalRequested = MutableStateFlow(false)
	private var isSettingsChangedObserver: SubscribedStateObservable<Boolean>? = null

	val accessCode = MutableStateObservable("")
	val libraryName = MutableStateObservable("")
	val userName = MutableStateObservable("")
	val password = MutableStateObservable("")
	val isLocalOnly = MutableStateObservable(false)
	val syncedFileLocation = MutableStateObservable(Library.SyncedFileLocation.INTERNAL)
	val isWakeOnLanEnabled = MutableStateObservable(false)
	val isUsingExistingFiles = MutableStateObservable(false)
	val isSyncLocalConnectionsOnly = MutableStateObservable(false)

	override val isLoading = mutableIsLoading.asStateFlow()
	val isSaving = mutableIsSaving.asStateFlow()
	val isStoragePermissionsNeeded = mutableIsPermissionsNeeded.asStateFlow()
	val isRemovalRequested = mutableIsRemovalRequested.asStateFlow()
	val isSettingsChanged: ReadOnlyStateObservable<Boolean>
		get() = isSettingsChangedObserver as? ReadOnlyStateObservable<Boolean> ?: SingleStateObservable(false)

	val activeLibraryId
		get() = library?.libraryId

	override fun onCleared() {
		isSettingsChangedObserver?.close()
	}

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

		isSettingsChangedObserver?.close()

		library = result?.copy(
			isLocalOnly = isLocalOnly.value,
			isUsingExistingFiles = isUsingExistingFiles.value,
			isSyncLocalConnectionsOnly = isSyncLocalConnectionsOnly.value,
			isWakeOnLanEnabled = isWakeOnLanEnabled.value,
			syncedFileLocation = syncedFileLocation.value,
			accessCode = accessCode.value,
			userName = userName.value,
			password = password.value,
			libraryName = libraryName.value,
		)

		isSettingsChangedObserver = SubscribedStateObservable(
			Observable.combineLatest(
				accessCode,
				libraryName,
				userName,
				password,
				isLocalOnly,
				syncedFileLocation,
				isWakeOnLanEnabled,
				isUsingExistingFiles,
				isSyncLocalConnectionsOnly
			) { newAccessCode, newLibraryName, newUserName, newPassword, newIsLocalOnly, newSyncedFileLocation, newWolEnabled, newUseExistingFile, newSyncLocal ->
				library?.run {
					accessCode != newAccessCode
						|| libraryName != newLibraryName
						|| userName != newUserName
						|| password != newPassword
						|| isLocalOnly != newIsLocalOnly
						|| syncedFileLocation != newSyncedFileLocation
						|| isWakeOnLanEnabled != newWolEnabled
						|| isUsingExistingFiles != newUseExistingFile
						|| isSyncLocalConnectionsOnly != newSyncLocal
				} ?: false
			},
			false
		)
	}

	override fun promiseResponse(resolution: Boolean): Promise<Boolean> {
		val isPermissionsNeeded = !resolution
		mutableIsPermissionsNeeded.value = isPermissionsNeeded

		if (isPermissionsNeeded) return false.toPromise()

		val localLibrary = library ?: return false.toPromise()

		return libraryStorage
			.saveLibrary(localLibrary)
			.then { true }
	}

	override fun act() {
		mutableIsLoading.value = false
		mutableIsSaving.value = false
	}
}
