package com.lasthopesoftware.bluewater.client.settings

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.access.RemoveLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredSubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.ProvideLibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.StoreLibrarySettings
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusExtensions.getConnectionStatusString
import com.lasthopesoftware.bluewater.permissions.RequestApplicationPermissions
import com.lasthopesoftware.bluewater.shared.NullBox
import com.lasthopesoftware.observables.InteractionState
import com.lasthopesoftware.observables.LiftedInteractionState
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.observables.mapNotNull
import com.lasthopesoftware.observables.toCloseable
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.onEach
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.AutoCloseableManager
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import io.reactivex.rxjava3.core.Observable

class LibrarySettingsViewModel(
	private val librarySettingsProvider: ProvideLibrarySettings,
	private val librarySettingsStorage: StoreLibrarySettings,
	private val libraryRemoval: RemoveLibraries,
	private val applicationPermissions: RequestApplicationPermissions,
	private val manageConnectionSessions: ManageConnectionSessions,
	private val stringResources: GetStringResources,
) :
	ViewModel(),
	TrackLoadedViewState,
	PromisedResponse<Boolean, Boolean>,
	ImmediateResponse<LibrarySettings?, Unit>,
	ImmediateAction
{
	companion object {

		private val defaultSubsonicConnectionSettings = StoredSubsonicConnectionSettings(
			url = "",
			userName = "",
			password = "",
			macAddress = "",
			sslCertificateFingerprint = "",
		)

		private val defaultMediaCenterConnectionSettings = StoredMediaCenterConnectionSettings(
			accessCode = "",
			userName = "",
			password = "",
			isSyncLocalConnectionsOnly = false,
			macAddress = "",
			sslCertificateFingerprint = "",
		)

		private val defaultLibrarySettings = LibrarySettings(
			libraryName = "",
			syncedFileLocation = SyncedFileLocation.INTERNAL,
		)
	}

	private val autoCloseables = AutoCloseableManager()

	private val mediaCenterConnectionSettingsViewModel by lazy {
		autoCloseables.manage(MediaCenterConnectionSettingsViewModel())
	}

	private val subsonicConnectionSettingsViewModel by lazy {
		autoCloseables.manage(SubsonicConnectionSettingsViewModel())
	}

	private val mutableSavedConnectionSettingsViewModel = MutableInteractionState<ConnectionSettingsViewModel<*>?>(null)

	private val libraryState = MutableInteractionState(defaultLibrarySettings.copy())
	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableIsSaving = MutableInteractionState(false)
	private val mutableIsPermissionsNeeded = MutableInteractionState(false)
	private val mutableIsRemovalRequested = MutableInteractionState(false)

	private val isSettingsChangedObserver by lazy {
		fun <T> observeLibraryChanges(observable: Observable<NullBox<T>>, libraryValue: LibrarySettings.() -> T?) =
			Observable.combineLatest(libraryState, observable) { l, o -> l.value.run(libraryValue) != o.value }.distinctUntilChanged()

		val changeTrackers = arrayOf(
			observeLibraryChanges(libraryName) { libraryName },
			observeLibraryChanges(isUsingExistingFiles) { isUsingExistingFiles },
			observeLibraryChanges(syncedFileLocation) { syncedFileLocation },
			Observable.combineLatest(mutableSavedConnectionSettingsViewModel, connectionSettingsViewModel) { a, b -> a.value !== b.value },
			mediaCenterConnectionSettingsViewModel.isConnectionSettingsChanged,
			subsonicConnectionSettingsViewModel.isConnectionSettingsChanged,
		)

		autoCloseables.manage(LiftedInteractionState(
			Observable.combineLatest(changeTrackers.asIterable()) { values -> values.any { it as Boolean } },
			false
		))
	}

	private val isTestingConnectionState = MutableInteractionState(false)
	private val isConnectionPossibleState = MutableInteractionState(false)
	private val connectionStatusState = MutableInteractionState("")

	val savedConnectionSettingsViewModel = mutableSavedConnectionSettingsViewModel.asInteractionState()
	val connectionSettingsViewModel = MutableInteractionState<ConnectionSettingsViewModel<*>?>(null)
	val libraryName = MutableInteractionState(defaultLibrarySettings.libraryName ?: "")
	val isUsingExistingFiles = MutableInteractionState(defaultLibrarySettings.isUsingExistingFiles)
	val syncedFileLocation = MutableInteractionState(defaultLibrarySettings.syncedFileLocation ?: SyncedFileLocation.INTERNAL)
	val connectionStatus = connectionStatusState.asInteractionState()
	val isTestingConnection = isTestingConnectionState.asInteractionState()
	val isConnectionPossible = isConnectionPossibleState.asInteractionState()

	override val isLoading = mutableIsLoading.asInteractionState()

	val isSaving = mutableIsSaving as InteractionState<Boolean>
	val isStoragePermissionsNeeded = mutableIsPermissionsNeeded as InteractionState<Boolean>
	val isRemovalRequested = mutableIsRemovalRequested as InteractionState<Boolean>
	val isSettingsChanged
		get() = isSettingsChangedObserver as InteractionState<Boolean>

	val activeLibraryId
		get() = libraryState.value.libraryId?.takeIf { it.id > -1 }

	val availableConnectionSettings by lazy {
		setOf(
			mediaCenterConnectionSettingsViewModel,
			subsonicConnectionSettingsViewModel,
		)
	}

	override fun onCleared() {
		autoCloseables.close()
	}

	fun loadLibrary(libraryId: LibraryId): Promise<*> {
		mutableIsLoading.value = true

		val promiseIsConnectionPossible = manageConnectionSessions
			.promiseIsConnectionActive(libraryId)
			.then { isActive -> isConnectionPossibleState.value = isActive }

		return Promise.whenAll(
			librarySettingsProvider.promiseLibrarySettings(libraryId).then(this),
			promiseIsConnectionPossible)
			.must(this)
	}

	fun saveLibrary(): Promise<Boolean> {
		mutableIsSaving.value = true
		val localLibrary = libraryState.value

		libraryState.value = localLibrary
			.copy(
				isUsingExistingFiles = isUsingExistingFiles.value,
				libraryName = libraryName.value,
				syncedFileLocation = syncedFileLocation.value,
				connectionSettings = connectionSettingsViewModel.value?.getCurrentConnectionSettings(),
			)

		return applicationPermissions
			.promiseIsAllPermissionsGranted(localLibrary)
			.eventually(this)
			.must(this)
	}

	fun saveAndTestLibrary(): Promise<Unit> {
		isTestingConnectionState.value = true
		isConnectionPossibleState.value = false
		return Promise.Proxy { cs ->
			saveLibrary()
				.eventually { isSaved ->
					if (!isSaved) Unit.toPromise()
					else activeLibraryId
						?.let(manageConnectionSessions::promiseTestedLibraryConnection)
						?.also(cs::doCancel)
						?.onEach { status ->
							connectionStatusState.value = stringResources.getConnectionStatusString(status)
						}
						?.then { c ->
							val isPossible = c != null
							isConnectionPossibleState.value = isPossible
						}
						.keepPromise(Unit)
				}
				.must { _ ->
					isTestingConnectionState.value = false
				}
		}
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

		mutableSavedConnectionSettingsViewModel.value = when (val parsedConnectionSettings = result?.connectionSettings) {
			is StoredMediaCenterConnectionSettings -> {
				mediaCenterConnectionSettingsViewModel.assignConnectionSettings(parsedConnectionSettings)
				mediaCenterConnectionSettingsViewModel
			}

			is StoredSubsonicConnectionSettings -> {
				subsonicConnectionSettingsViewModel.assignConnectionSettings(parsedConnectionSettings)
				subsonicConnectionSettingsViewModel
			}

			null -> {
				null
			}
		}

		connectionSettingsViewModel.value = mutableSavedConnectionSettingsViewModel.value

		libraryState.value = (result ?: defaultLibrarySettings).copy(
			isUsingExistingFiles = isUsingExistingFiles.value,
			libraryName = libraryName.value,
			syncedFileLocation = syncedFileLocation.value,
			connectionSettings = mutableSavedConnectionSettingsViewModel.value?.getCurrentConnectionSettings(),
		)
	}

	override fun promiseResponse(resolution: Boolean): Promise<Boolean> {
		val isPermissionsNeeded = !resolution
		mutableIsPermissionsNeeded.value = isPermissionsNeeded

		if (isPermissionsNeeded) return false.toPromise()

		val localLibrary = libraryState.value

		return librarySettingsStorage
			.promiseSavedLibrarySettings(localLibrary)
			.then {
				libraryState.value = it

				mutableSavedConnectionSettingsViewModel.value = when (val parsedConnectionSettings = it?.connectionSettings) {
					is StoredMediaCenterConnectionSettings -> {
						mediaCenterConnectionSettingsViewModel.assignConnectionSettings(parsedConnectionSettings)
						mediaCenterConnectionSettingsViewModel
					}

					is StoredSubsonicConnectionSettings -> {
						subsonicConnectionSettingsViewModel.assignConnectionSettings(parsedConnectionSettings)
						subsonicConnectionSettingsViewModel
					}

					null -> {
						null
					}
				}

				true
			}
	}

	override fun act() {
		mutableIsLoading.value = false
		mutableIsSaving.value = false
	}

	private inline fun <reified C : StoredConnectionSettings, T> observeConnectionSettingsChanges(observable: Observable<NullBox<T>>, crossinline connectionValue: C.() -> T?) =
		Observable.combineLatest(libraryState, observable) { s, o ->
			s.value.connectionSettings
				?.let { it as? C }
				?.run(connectionValue)
				?.let { it != o.value }
				?: false
		}.distinctUntilChanged()

	interface ConnectionSettingsViewModel<ConnectionSettings : StoredConnectionSettings> : AutoCloseable {
		val connectionTypeName: String
		val isConnectionSettingsChanged: Observable<Boolean>
		fun getCurrentConnectionSettings(): StoredConnectionSettings
		fun assignConnectionSettings(connectionSettings: ConnectionSettings)
	}

	@OptIn(ExperimentalStdlibApi::class)
	inner class MediaCenterConnectionSettingsViewModel :
		ConnectionSettingsViewModel<StoredMediaCenterConnectionSettings>
	{
		private val autoCloseables = AutoCloseableManager()

		override val connectionTypeName: String
			get() = stringResources.mediaCenter

		override val isConnectionSettingsChanged: Observable<Boolean> by lazy {
			val changeTrackers = arrayOf(
				observeConnectionSettingsChanges(accessCode) { accessCode },
				observeConnectionSettingsChanges(userName) { userName },
				observeConnectionSettingsChanges(password) { password },
				observeConnectionSettingsChanges(isLocalOnly) { isLocalOnly },
				observeConnectionSettingsChanges(isWakeOnLanEnabled) { isWakeOnLanEnabled },
				observeConnectionSettingsChanges(isSyncLocalConnectionsOnly) { isSyncLocalConnectionsOnly },
				observeConnectionSettingsChanges(mutableSslStringCertificate) { sslCertificateFingerprint },
				observeConnectionSettingsChanges(macAddress) { macAddress },
			)

			Observable.combineLatest(changeTrackers.asIterable()) { values -> values.any { it as Boolean } }
		}

		private val mutableSslStringCertificate =
			MutableInteractionState(defaultMediaCenterConnectionSettings.sslCertificateFingerprint ?: "")

		private val hasSslCertificateObserver = lazy {
			autoCloseables.manage(LiftedInteractionState(sslCertificateFingerprint.map { it.value.any() }, false))
		}

		val accessCode = MutableInteractionState(defaultMediaCenterConnectionSettings.accessCode ?: "")
		val userName = MutableInteractionState(defaultMediaCenterConnectionSettings.userName ?: "")
		val password = MutableInteractionState(defaultMediaCenterConnectionSettings.password ?: "")
		val isLocalOnly = MutableInteractionState(defaultMediaCenterConnectionSettings.isLocalOnly)
		val isWakeOnLanEnabled = MutableInteractionState(defaultMediaCenterConnectionSettings.isWakeOnLanEnabled)
		val isSyncLocalConnectionsOnly = MutableInteractionState(defaultMediaCenterConnectionSettings.isSyncLocalConnectionsOnly)
		val sslCertificateFingerprint = MutableInteractionState(mutableSslStringCertificate.value.hexToByteArray())
		val macAddress = MutableInteractionState(defaultMediaCenterConnectionSettings.macAddress ?: "")
		val hasSslCertificate
			get() = hasSslCertificateObserver.value as InteractionState<Boolean>

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

		override fun close() {
			autoCloseables.close()
		}

		private inline fun <T> observeConnectionSettingsChanges(observable: Observable<NullBox<T>>, crossinline connectionValue: StoredMediaCenterConnectionSettings.() -> T?) =
			observeConnectionSettingsChanges<StoredMediaCenterConnectionSettings, T>(observable, connectionValue)

		override fun assignConnectionSettings(connectionSettings: StoredMediaCenterConnectionSettings) {
			isWakeOnLanEnabled.value = connectionSettings.isWakeOnLanEnabled
			isSyncLocalConnectionsOnly.value = connectionSettings.isSyncLocalConnectionsOnly
			accessCode.value = connectionSettings.accessCode ?: ""
			userName.value = connectionSettings.userName ?: ""
			password.value = connectionSettings.password ?: ""
			isLocalOnly.value = connectionSettings.isLocalOnly
			macAddress.value = connectionSettings.macAddress ?: ""
			mutableSslStringCertificate.value = connectionSettings.sslCertificateFingerprint ?: ""
		}

		override fun getCurrentConnectionSettings() = StoredMediaCenterConnectionSettings(
			isWakeOnLanEnabled = isWakeOnLanEnabled.value,
			isSyncLocalConnectionsOnly = isSyncLocalConnectionsOnly.value,
			accessCode = accessCode.value,
			userName = userName.value,
			password = password.value,
			isLocalOnly = isLocalOnly.value,
			macAddress = macAddress.value,
			sslCertificateFingerprint = mutableSslStringCertificate.value,
		)
	}

	@OptIn(ExperimentalStdlibApi::class)
	inner class SubsonicConnectionSettingsViewModel : ConnectionSettingsViewModel<StoredSubsonicConnectionSettings> {
		private val autoCloseables = AutoCloseableManager()

		override val connectionTypeName: String
			get() = stringResources.subsonic

		override val isConnectionSettingsChanged: Observable<Boolean> by lazy {
			val changeTrackers = arrayOf(
				observeConnectionSettingsChanges(url) { url },
				observeConnectionSettingsChanges(userName) { userName },
				observeConnectionSettingsChanges(password) { password },
				observeConnectionSettingsChanges(isWakeOnLanEnabled) { isWakeOnLanEnabled },
				observeConnectionSettingsChanges(mutableSslStringCertificate) { sslCertificateFingerprint },
				observeConnectionSettingsChanges(macAddress) { macAddress },
			)

			Observable.combineLatest(changeTrackers.asIterable()) { values -> values.any { it as Boolean } }
		}

		private val mutableSslStringCertificate =
			MutableInteractionState(defaultSubsonicConnectionSettings.sslCertificateFingerprint ?: "")

		private val hasSslCertificateObserver = lazy {
			autoCloseables.manage(LiftedInteractionState(sslCertificateFingerprint.map { it.value.any() }, false))
		}

		val url = MutableInteractionState(defaultSubsonicConnectionSettings.url ?: "")
		val userName = MutableInteractionState(defaultSubsonicConnectionSettings.userName ?: "")
		val password = MutableInteractionState(defaultSubsonicConnectionSettings.password ?: "")
		val isWakeOnLanEnabled = MutableInteractionState(defaultSubsonicConnectionSettings.isWakeOnLanEnabled)
		val sslCertificateFingerprint = MutableInteractionState(mutableSslStringCertificate.value.hexToByteArray())
		val macAddress = MutableInteractionState(defaultSubsonicConnectionSettings.macAddress ?: "")
		val hasSslCertificate
			get() = hasSslCertificateObserver.value as InteractionState<Boolean>

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

		override fun close() {
			autoCloseables.close()
		}

		private inline fun <T> observeConnectionSettingsChanges(observable: Observable<NullBox<T>>, crossinline connectionValue: StoredSubsonicConnectionSettings.() -> T?) =
			observeConnectionSettingsChanges<StoredSubsonicConnectionSettings, T>(observable, connectionValue)

		override fun assignConnectionSettings(connectionSettings: StoredSubsonicConnectionSettings) {
			isWakeOnLanEnabled.value = connectionSettings.isWakeOnLanEnabled
			url.value = connectionSettings.url ?: ""
			userName.value = connectionSettings.userName ?: ""
			password.value = connectionSettings.password ?: ""
			macAddress.value = connectionSettings.macAddress ?: ""
			mutableSslStringCertificate.value = connectionSettings.sslCertificateFingerprint ?: ""
		}

		override fun getCurrentConnectionSettings() = StoredSubsonicConnectionSettings(
			isWakeOnLanEnabled = isWakeOnLanEnabled.value,
			url = url.value,
			userName = userName.value,
			password = password.value,
			macAddress = macAddress.value,
			sslCertificateFingerprint = mutableSslStringCertificate.value,
		)
	}
}
