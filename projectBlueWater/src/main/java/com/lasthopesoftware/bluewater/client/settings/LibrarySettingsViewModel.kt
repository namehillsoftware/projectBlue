package com.lasthopesoftware.bluewater.client.settings

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.access.RemoveLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.ProvideLibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.StoreLibrarySettings
import com.lasthopesoftware.bluewater.permissions.RequestApplicationPermissions
import com.lasthopesoftware.bluewater.shared.NullBox
import com.lasthopesoftware.bluewater.shared.observables.InteractionState
import com.lasthopesoftware.bluewater.shared.observables.LiftedInteractionState
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
import com.lasthopesoftware.bluewater.shared.observables.toCloseable
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.AutoCloseableManager
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import io.reactivex.rxjava3.core.Observable

@OptIn(ExperimentalStdlibApi::class)
class LibrarySettingsViewModel(
	private val librarySettingsProvider: ProvideLibrarySettings,
	private val librarySettingsStorage: StoreLibrarySettings,
	private val libraryRemoval: RemoveLibraries,
	private val applicationPermissions: RequestApplicationPermissions,
) :
	ViewModel(),
	TrackLoadedViewState,
	PromisedResponse<Boolean, Boolean>,
	ImmediateResponse<LibrarySettings?, Unit>,
	ImmediateAction
{

	companion object {

		private val defaultConnectionSettings = StoredMediaCenterConnectionSettings(
			accessCode = "",
			userName = "",
			password = "",
			isSyncLocalConnectionsOnly = false,
			macAddress = "",
			sslCertificateFingerprint = "",
		)

		private val defaultLibrarySettings = LibrarySettings(
			libraryName = "",
			connectionSettings = defaultConnectionSettings,
			syncedFileLocation = SyncedFileLocation.INTERNAL,
		)
	}

	private val autoCloseables = AutoCloseableManager()
	private val libraryState = MutableInteractionState(defaultLibrarySettings.copy())
	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableIsSaving = MutableInteractionState(false)
	private val mutableIsPermissionsNeeded = MutableInteractionState(false)
	private val mutableIsRemovalRequested = MutableInteractionState(false)

	private val isSettingsChangedObserver = lazy {
		fun <T> observeLibraryChanges(observable: Observable<NullBox<T>>, libraryValue: LibrarySettings.() -> T?) =
			Observable.combineLatest(libraryState, observable) { l, o -> l.value.run(libraryValue) != o.value }

		fun <T> observeConnectionSettingsChanges(observable: Observable<NullBox<T>>, connectionValue: StoredMediaCenterConnectionSettings.() -> T?) =
			Observable.combineLatest(libraryState, observable) { l, o -> l.value.connectionSettings?.run(connectionValue) != o.value }

		val changeTrackers = arrayOf(
			observeLibraryChanges(libraryName) { libraryName },
			observeLibraryChanges(isUsingExistingFiles) { isUsingExistingFiles },
			observeLibraryChanges(syncedFileLocation) { syncedFileLocation },
			observeConnectionSettingsChanges(accessCode) { accessCode },
			observeConnectionSettingsChanges(userName) { userName },
			observeConnectionSettingsChanges(password) { password },
			observeConnectionSettingsChanges(isLocalOnly) { isLocalOnly },
			observeConnectionSettingsChanges(isWakeOnLanEnabled) { isWakeOnLanEnabled },
			observeConnectionSettingsChanges(isSyncLocalConnectionsOnly) { isSyncLocalConnectionsOnly },
			observeConnectionSettingsChanges(mutableSslStringCertificate) { sslCertificateFingerprint },
			observeConnectionSettingsChanges(macAddress) { macAddress },
		)

		autoCloseables.manage(LiftedInteractionState(
			Observable.combineLatest(changeTrackers.asIterable()) { values -> values.any { it as Boolean } },
			false
		))
	}

	private val mutableSslStringCertificate = MutableInteractionState(defaultConnectionSettings.sslCertificateFingerprint ?: "")

	private val hasSslCertificateObserver = lazy {
		autoCloseables.manage(LiftedInteractionState(sslCertificateFingerprint.map { it.value.any() }, false))
	}

	val libraryName = MutableInteractionState(defaultLibrarySettings.libraryName ?: "")
	val isUsingExistingFiles = MutableInteractionState(defaultLibrarySettings.isUsingExistingFiles)
	val syncedFileLocation = MutableInteractionState(defaultLibrarySettings.syncedFileLocation ?: SyncedFileLocation.INTERNAL)
	val accessCode = MutableInteractionState(defaultConnectionSettings.accessCode ?: "")
	val userName = MutableInteractionState(defaultConnectionSettings.userName ?: "")
	val password = MutableInteractionState(defaultConnectionSettings.password ?: "")
	val isLocalOnly = MutableInteractionState(defaultConnectionSettings.isLocalOnly)
	val isWakeOnLanEnabled = MutableInteractionState(defaultConnectionSettings.isWakeOnLanEnabled)
	val isSyncLocalConnectionsOnly = MutableInteractionState(defaultConnectionSettings.isSyncLocalConnectionsOnly)
	val sslCertificateFingerprint = MutableInteractionState(mutableSslStringCertificate.value.hexToByteArray())
	val macAddress = MutableInteractionState(defaultConnectionSettings.macAddress ?: "")
	val hasSslCertificate
		get() = hasSslCertificateObserver.value as InteractionState<Boolean>

	override val isLoading = mutableIsLoading.asInteractionState()
	val isSaving = mutableIsSaving as InteractionState<Boolean>
	val isStoragePermissionsNeeded = mutableIsPermissionsNeeded as InteractionState<Boolean>
	val isRemovalRequested = mutableIsRemovalRequested as InteractionState<Boolean>
	val isSettingsChanged
		get() = isSettingsChangedObserver.value as InteractionState<Boolean>

	val activeLibraryId
		get() = libraryState.value.libraryId?.takeIf { it.id > -1 }

	init {
		autoCloseables.manage(
			sslCertificateFingerprint.mapNotNull().subscribe { c ->
				mutableSslStringCertificate.value = c.toHexString()
			}.toCloseable()
		)

		autoCloseables.manage(
			mutableSslStringCertificate.mapNotNull().subscribe { s ->
				sslCertificateFingerprint.value = s.hexToByteArray()
			}.toCloseable()
		)
	}

	override fun onCleared() {
		autoCloseables.close()
	}

	fun loadLibrary(libraryId: LibraryId): Promise<*> {
		mutableIsLoading.value = true

		return librarySettingsProvider
			.promiseLibrarySettings(libraryId)
			.then(this)
			.must(this)
	}

	fun saveLibrary(): Promise<Boolean> {
		mutableIsSaving.value = true
		val localLibrary = libraryState.value

		val localConnectionSettings = StoredMediaCenterConnectionSettings(
			accessCode = accessCode.value,
			userName = userName.value,
			password = password.value,
			isSyncLocalConnectionsOnly = isSyncLocalConnectionsOnly.value,
			isLocalOnly = isLocalOnly.value,
			isWakeOnLanEnabled = isWakeOnLanEnabled.value,
			sslCertificateFingerprint = mutableSslStringCertificate.value,
			macAddress = macAddress.value,
		)

		libraryState.value = localLibrary
			.copy(
				isUsingExistingFiles = isUsingExistingFiles.value,
				libraryName = libraryName.value,
				syncedFileLocation = syncedFileLocation.value,
				connectionSettings = localConnectionSettings
			)

		return applicationPermissions
			.promiseIsAllPermissionsGranted(localLibrary)
			.eventually(this)
			.must(this)
	}

	fun requestLibraryRemoval() {
		mutableIsRemovalRequested.value = true
	}

	fun cancelLibraryRemovalRequest() {
		mutableIsRemovalRequested.value = false
	}

	fun removeLibrary(): Promise<*> = libraryState.value.libraryId
		.takeIf { mutableIsRemovalRequested.value }
		?.let(libraryRemoval::removeLibrary)
		.keepPromise()

	override fun respond(result: LibrarySettings?) {
		isUsingExistingFiles.value = result?.isUsingExistingFiles ?: false
		libraryName.value = result?.libraryName ?: ""
		syncedFileLocation.value = result?.syncedFileLocation ?: SyncedFileLocation.INTERNAL

		val parsedConnectionSettings = result?.connectionSettings ?: defaultConnectionSettings.copy()
		isWakeOnLanEnabled.value = parsedConnectionSettings.isWakeOnLanEnabled
		isSyncLocalConnectionsOnly.value = parsedConnectionSettings.isSyncLocalConnectionsOnly
		accessCode.value = parsedConnectionSettings.accessCode ?: ""
		userName.value = parsedConnectionSettings.userName ?: ""
		password.value = parsedConnectionSettings.password ?: ""
		isLocalOnly.value = parsedConnectionSettings.isLocalOnly
		macAddress.value = parsedConnectionSettings.macAddress ?: ""
		mutableSslStringCertificate.value = parsedConnectionSettings.sslCertificateFingerprint ?: ""

		libraryState.value = (result ?: defaultLibrarySettings).copy(
			isUsingExistingFiles = isUsingExistingFiles.value,
			libraryName = libraryName.value,
			syncedFileLocation = syncedFileLocation.value,
			connectionSettings = StoredMediaCenterConnectionSettings(
				isWakeOnLanEnabled = isWakeOnLanEnabled.value,
				isSyncLocalConnectionsOnly = isSyncLocalConnectionsOnly.value,
				accessCode = accessCode.value,
				userName = userName.value,
				password = password.value,
				isLocalOnly = isLocalOnly.value,
				macAddress = macAddress.value,
				sslCertificateFingerprint = mutableSslStringCertificate.value,
			)
		)
	}

	override fun promiseResponse(resolution: Boolean): Promise<Boolean> {
		val isPermissionsNeeded = !resolution
		mutableIsPermissionsNeeded.value = isPermissionsNeeded

		if (isPermissionsNeeded) return false.toPromise()

		val localLibrary = libraryState.value

		return librarySettingsStorage
			.promiseSavedLibrarySettings(localLibrary)
			.then { it ->
				libraryState.value = it
				true
			}
	}

	override fun act() {
		mutableIsLoading.value = false
		mutableIsSaving.value = false
	}
}
