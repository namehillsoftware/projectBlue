package com.lasthopesoftware.bluewater.client.browsing.library.settings.access

import com.lasthopesoftware.bluewater.client.browsing.library.access.ManageLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.PasswordStoredConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredSubsonicConnectionSettings
import com.lasthopesoftware.encryption.EncryptionConfiguration
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.strings.EncryptedString
import com.lasthopesoftware.resources.strings.GuardStrings
import com.lasthopesoftware.resources.strings.TranslateJson
import com.lasthopesoftware.resources.strings.parseJson
import com.namehillsoftware.handoff.promises.Promise
import kotlin.coroutines.cancellation.CancellationException

class LibrarySettingsAccess(
	private val libraryManager: ManageLibraries,
	private val jsonTranslator: TranslateJson,
	private val stringGuard: GuardStrings,
) : ProvideLibrarySettings, StoreLibrarySettings {
	companion object {
		private val serverTypeNames by lazy { Library.ServerType.entries.map { it.name } }

		private fun librarySettingsAccessCancelled() = CancellationException("Cancelled while accessing Library Settings.")
	}

	override fun promiseAllLibrarySettings(): Promise<Collection<LibrarySettings>> =
		libraryManager
			.promiseAllLibraries()
			.cancelBackEventually { libraries ->
				Promise.whenAll(libraries.map { l -> l.promiseDecryptedLibrarySettings() })
			}

	override fun promiseLibrarySettings(libraryId: LibraryId): Promise<LibrarySettings?> =
		libraryManager
			.promiseLibrary(libraryId)
			.cancelBackEventually { maybeLibrary ->
				maybeLibrary
					?.promiseDecryptedLibrarySettings()
					.keepPromise()
			}

	override fun promiseSavedLibrarySettings(librarySettings: LibrarySettings): Promise<LibrarySettings> =
		(librarySettings.libraryId
			?.let(libraryManager::promiseLibrary)
			?.cancelBackEventually { maybeLibrary ->
				maybeLibrary
					?.let { l ->
						val connectionSettings = librarySettings.connectionSettings

						l.libraryName = librarySettings.libraryName
						l.isUsingExistingFiles = librarySettings.isUsingExistingFiles
						l.syncedFileLocation = librarySettings.syncedFileLocation

						l.serverType = when (connectionSettings) {
							is StoredMediaCenterConnectionSettings -> Library.ServerType.MediaCenter.name
							is StoredSubsonicConnectionSettings -> Library.ServerType.Subsonic.name
							null -> null
						}

						connectionSettings
							?.let(::encryptPassword)
							?.eventually { encryptedString ->
								val encryptedSettings = when (connectionSettings) {
									is StoredMediaCenterConnectionSettings -> {
										connectionSettings.copy(
											initializationVector = encryptedString?.initializationVector,
											password = encryptedString?.protectedString,
											encryptionConfiguration = encryptedString?.let(::EncryptionConfiguration)
										)
									}
									is StoredSubsonicConnectionSettings -> {
										connectionSettings.copy(
											initializationVector = encryptedString?.initializationVector,
											password = encryptedString?.protectedString,
											encryptionConfiguration = encryptedString?.let(::EncryptionConfiguration)
										)
									}
								}

								ThreadPools.compute.preparePromise { cs ->
									if (cs.isCancelled) throw librarySettingsAccessCancelled()

									l.connectionSettings = encryptedSettings.let(jsonTranslator::toJson)
									l
								}
							}
							?: ThreadPools.compute.preparePromise { cs ->
								if (cs.isCancelled) throw librarySettingsAccessCancelled()

								l.serverType = null
								l.connectionSettings = null
								l
							}
					}
					?: ThreadPools.compute.preparePromise { cs ->
						if (cs.isCancelled) throw librarySettingsAccessCancelled()
						librarySettings.toNewLibrary()
					}
			}
			?: ThreadPools.compute.preparePromise { cs ->
				if (cs.isCancelled) throw librarySettingsAccessCancelled()
				librarySettings.toNewLibrary()
			})
			.cancelBackEventually(libraryManager::saveLibrary)
			.cancelBackEventually { it.promiseDecryptedLibrarySettings() }

	private fun LibrarySettings.toNewLibrary() = Library(
		libraryName = libraryName,
		isUsingExistingFiles = isUsingExistingFiles,
		serverType = when (connectionSettings) {
			is StoredMediaCenterConnectionSettings -> Library.ServerType.MediaCenter.name
			is StoredSubsonicConnectionSettings -> Library.ServerType.Subsonic.name
			null -> null
		},
		syncedFileLocation = syncedFileLocation,
		connectionSettings = connectionSettings?.let(jsonTranslator::toJson),
	)

	private fun Library.promiseDecryptedLibrarySettings(): Promise<LibrarySettings> =
		ThreadPools.compute.preparePromise { cs ->
			if (cs.isCancelled) throw librarySettingsAccessCancelled()

			toLibrarySettings()
		}
		.cancelBackEventually { librarySettings ->
			val connectionSettings = librarySettings.connectionSettings as? PasswordStoredConnectionSettings
			connectionSettings
				?.takeIf { it.password != null }
				?.run {
					if (initializationVector != null && encryptionConfiguration != null) librarySettings
						.promiseDecryptedPassword()
						.cancelBackThen { decryptedPassword, _ ->
							librarySettings.copy(
								connectionSettings = when (connectionSettings) {
									is StoredMediaCenterConnectionSettings -> connectionSettings.copy(password = decryptedPassword)
									is StoredSubsonicConnectionSettings -> connectionSettings.copy(password = decryptedPassword)
								}
							)
						}
					else promiseSavedLibrarySettings(librarySettings)
				}
				?: librarySettings.toPromise()
		}

	private fun Library.toLibrarySettings() = LibrarySettings(
		libraryId = libraryId,
		isUsingExistingFiles = isUsingExistingFiles,
		libraryName = libraryName,
		syncedFileLocation = syncedFileLocation,
		connectionSettings = connectionSettings
			?.takeIf { serverTypeNames.contains(serverType) }
			?.let { settings ->
				serverType?.let {
					when (Library.ServerType.valueOf(it)) {
						Library.ServerType.MediaCenter -> jsonTranslator.parseJson<StoredMediaCenterConnectionSettings>(settings)
						Library.ServerType.Subsonic -> jsonTranslator.parseJson<StoredSubsonicConnectionSettings>(settings)
					}
				}
			}
	)

	@Suppress("UNCHECKED_CAST")
	private fun LibrarySettings.promiseDecryptedPassword(): Promise<String?> {
		val connectionSettings = connectionSettings as? PasswordStoredConnectionSettings ?: return Promise.empty()
		val password = connectionSettings.password ?: return Promise.empty()

		val encryptionConfiguration = connectionSettings.encryptionConfiguration ?: return Promise.empty()
		val initializationVector = connectionSettings.initializationVector ?: return Promise.empty()
//		if (encryptionConfiguration == null || initializationVector == null) {
//			return promiseSavedLibrarySettings(this).cancelBackEventually { it.promiseDecryptedPassword() }
//		}

		return stringGuard.promiseDecryption(
			EncryptedString(
				initializationVector,
				password,
				encryptionConfiguration.algorithm,
				encryptionConfiguration.blockMode,
				encryptionConfiguration.padding
			)
		) as Promise<String?>
	}

	private fun encryptPassword(connectionSettings: PasswordStoredConnectionSettings): Promise<EncryptedString?> =
		connectionSettings.password?.let(stringGuard::promiseEncryption).keepPromise()
}
