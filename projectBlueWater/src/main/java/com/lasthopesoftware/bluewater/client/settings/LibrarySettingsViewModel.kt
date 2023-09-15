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
import com.lasthopesoftware.bluewater.shared.NullBox
import com.lasthopesoftware.bluewater.shared.observables.MutableStateObservable
import com.lasthopesoftware.bluewater.shared.observables.ReadOnlyStateObservable
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
	companion object {
		private val defaultLibrary = Library(
			id = -1,
			nowPlayingId = -1,
			accessCode = "",
			libraryName = "",
			userName = "",
			password = "",
			syncedFileLocation = Library.SyncedFileLocation.INTERNAL,
		)
	}

	private val libraryState = MutableStateObservable(defaultLibrary.copy())
	private val mutableIsLoading = MutableStateFlow(false)
	private val mutableIsSaving = MutableStateFlow(false)
	private val mutableIsPermissionsNeeded = MutableStateFlow(false)
	private val mutableIsRemovalRequested = MutableStateFlow(false)

	private val changeTrackers by lazy {
		fun <T> observeChanges(observable: Observable<NullBox<T>>, libraryValue: Library.() -> T?) =
			Observable.combineLatest(libraryState, observable) { l, o -> l.value.run(libraryValue) != o.value }

		arrayOf(
			observeChanges(accessCode) { accessCode },
			observeChanges(libraryName) { libraryName },
			observeChanges(userName) { userName },
			observeChanges(password) { password },
			observeChanges(isLocalOnly) { isLocalOnly },
			observeChanges(syncedFileLocation) { syncedFileLocation },
			observeChanges(isWakeOnLanEnabled) { isWakeOnLanEnabled },
			observeChanges(isUsingExistingFiles) { isUsingExistingFiles },
			observeChanges(isSyncLocalConnectionsOnly) { isSyncLocalConnectionsOnly }
		)
	}

	private var isSettingsChangedObserver = lazy {
		SubscribedStateObservable(
			Observable.combineLatest(changeTrackers) { values -> values.any { it as Boolean } },
			false
		)
	}

	val accessCode = MutableStateObservable(defaultLibrary.accessCode ?: "")
	val libraryName = MutableStateObservable(defaultLibrary.libraryName ?: "")
	val userName = MutableStateObservable(defaultLibrary.userName ?: "")
	val password = MutableStateObservable(defaultLibrary.password ?: "")
	val isLocalOnly = MutableStateObservable(defaultLibrary.isLocalOnly)
	val syncedFileLocation = MutableStateObservable(defaultLibrary.syncedFileLocation ?: Library.SyncedFileLocation.INTERNAL)
	val isWakeOnLanEnabled = MutableStateObservable(defaultLibrary.isWakeOnLanEnabled)
	val isUsingExistingFiles = MutableStateObservable(defaultLibrary.isUsingExistingFiles)
	val isSyncLocalConnectionsOnly = MutableStateObservable(defaultLibrary.isSyncLocalConnectionsOnly)

	override val isLoading = mutableIsLoading.asStateFlow()
	val isSaving = mutableIsSaving.asStateFlow()
	val isStoragePermissionsNeeded = mutableIsPermissionsNeeded.asStateFlow()
	val isRemovalRequested = mutableIsRemovalRequested.asStateFlow()
	val isSettingsChanged: ReadOnlyStateObservable<Boolean>
		get() = isSettingsChangedObserver.value

	val activeLibraryId
		get() = libraryState.value.libraryId

	override fun onCleared() {
		if (isSettingsChangedObserver.isInitialized())
			isSettingsChangedObserver.value.close()
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
		val localLibrary = libraryState.value

		libraryState.value = localLibrary
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

	fun removeLibrary(): Promise<*> = libraryState.value.takeIf { mutableIsRemovalRequested.value }?.let(libraryRemoval::removeLibrary).keepPromise()

	override fun respond(result: Library?) {
		isLocalOnly.value = result?.isLocalOnly ?: false
		isUsingExistingFiles.value = result?.isUsingExistingFiles ?: false
		isSyncLocalConnectionsOnly.value = result?.isSyncLocalConnectionsOnly ?: false
		isWakeOnLanEnabled.value = result?.isWakeOnLanEnabled ?: false

		syncedFileLocation.value = result?.syncedFileLocation ?: Library.SyncedFileLocation.INTERNAL

		accessCode.value = result?.accessCode ?: ""
		userName.value = result?.userName ?: ""
		password.value = result?.password ?: ""
		libraryName.value = result?.libraryName ?: ""

		libraryState.value = result?.copy(
			isLocalOnly = isLocalOnly.value,
			isUsingExistingFiles = isUsingExistingFiles.value,
			isSyncLocalConnectionsOnly = isSyncLocalConnectionsOnly.value,
			isWakeOnLanEnabled = isWakeOnLanEnabled.value,
			syncedFileLocation = syncedFileLocation.value,
			accessCode = accessCode.value,
			userName = userName.value,
			password = password.value,
			libraryName = libraryName.value,
		) ?: defaultLibrary.copy()
	}

	override fun promiseResponse(resolution: Boolean): Promise<Boolean> {
		val isPermissionsNeeded = !resolution
		mutableIsPermissionsNeeded.value = isPermissionsNeeded

		if (isPermissionsNeeded) return false.toPromise()

		val localLibrary = libraryState.value

		return libraryStorage
			.saveLibrary(localLibrary)
			.then {
				libraryState.value = it
				true
			}
	}

	override fun act() {
		mutableIsLoading.value = false
		mutableIsSaving.value = false
	}
}
