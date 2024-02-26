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
import com.lasthopesoftware.bluewater.shared.observables.InteractionState
import com.lasthopesoftware.bluewater.shared.observables.LiftedInteractionState
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import io.reactivex.rxjava3.core.Observable

class LibrarySettingsViewModel(
	private val libraryProvider: ILibraryProvider,
	private val libraryStorage: ILibraryStorage,
	private val libraryRemoval: RemoveLibraries,
	private val applicationPermissions: RequestApplicationPermissions,
) : ViewModel(), PromisedResponse<Boolean, Boolean>, ImmediateResponse<Library?, Unit>, TrackLoadedViewState, ImmediateAction {

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

	private val libraryState = MutableInteractionState(defaultLibrary.copy())
	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableIsSaving = MutableInteractionState(false)
	private val mutableIsPermissionsNeeded = MutableInteractionState(false)
	private val mutableIsRemovalRequested = MutableInteractionState(false)

	private val isSettingsChangedObserver = lazy {
		fun <T> observeChanges(observable: Observable<NullBox<T>>, libraryValue: Library.() -> T?) =
			Observable.combineLatest(libraryState, observable) { l, o -> l.value.run(libraryValue) != o.value }

		val changeTrackers = arrayOf(
			observeChanges(accessCode) { accessCode },
			observeChanges(libraryName) { libraryName },
			observeChanges(userName) { userName },
			observeChanges(password) { password },
			observeChanges(isLocalOnly) { isLocalOnly },
			observeChanges(syncedFileLocation) { syncedFileLocation },
			observeChanges(isWakeOnLanEnabled) { isWakeOnLanEnabled },
			observeChanges(isUsingExistingFiles) { isUsingExistingFiles },
			observeChanges(isSyncLocalConnectionsOnly) { isSyncLocalConnectionsOnly },
			Observable.combineLatest(libraryState, sslCertificateFingerprint) { l, f -> !f.value.contentEquals(l.value.sslCertificateFingerprint) },
		)

		LiftedInteractionState(
			Observable.combineLatest(changeTrackers.asIterable()) { values -> values.any { it as Boolean } },
			false
		)
	}

	private val hasSslCertificateObserver = lazy {
		LiftedInteractionState(sslCertificateFingerprint.map { it.value.any() }, false)
	}

	val accessCode = MutableInteractionState(defaultLibrary.accessCode ?: "")
	val libraryName = MutableInteractionState(defaultLibrary.libraryName ?: "")
	val userName = MutableInteractionState(defaultLibrary.userName ?: "")
	val password = MutableInteractionState(defaultLibrary.password ?: "")
	val isLocalOnly = MutableInteractionState(defaultLibrary.isLocalOnly)
	val syncedFileLocation = MutableInteractionState(defaultLibrary.syncedFileLocation ?: Library.SyncedFileLocation.INTERNAL)
	val isWakeOnLanEnabled = MutableInteractionState(defaultLibrary.isWakeOnLanEnabled)
	val isUsingExistingFiles = MutableInteractionState(defaultLibrary.isUsingExistingFiles)
	val isSyncLocalConnectionsOnly = MutableInteractionState(defaultLibrary.isSyncLocalConnectionsOnly)
	val sslCertificateFingerprint = MutableInteractionState(ByteArray(0))
	val hasSslCertificate
		get() = hasSslCertificateObserver.value as InteractionState<Boolean>

	override val isLoading = mutableIsLoading.asInteractionState()
	val isSaving = mutableIsSaving as InteractionState<Boolean>
	val isStoragePermissionsNeeded = mutableIsPermissionsNeeded as InteractionState<Boolean>
	val isRemovalRequested = mutableIsRemovalRequested as InteractionState<Boolean>
	val isSettingsChanged
		get() = isSettingsChangedObserver.value as InteractionState<Boolean>

	val activeLibraryId
		get() = libraryState.value.libraryId.takeIf { it.id > -1 }

	override fun onCleared() {
		if (isSettingsChangedObserver.isInitialized())
			isSettingsChangedObserver.value.close()

		if (hasSslCertificateObserver.isInitialized())
			hasSslCertificateObserver.value.close()
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
				libraryName = libraryName.value,
				sslCertificateFingerprint = sslCertificateFingerprint.value
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
		sslCertificateFingerprint.value = result?.sslCertificateFingerprint ?: ByteArray(0)

		libraryState.value = (result ?: defaultLibrary).copy(
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
