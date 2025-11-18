package com.lasthopesoftware.bluewater.client.browsing.library.settings.access

import com.lasthopesoftware.bluewater.client.browsing.library.access.ManageLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.PasswordStoredConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.PasswordStoredConnectionSettings.Companion.copy
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredEncryptionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredSubsonicConnectionSettings
import com.lasthopesoftware.encryption.EncryptionConfiguration
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.strings.TranslateGson
import com.lasthopesoftware.resources.strings.TranslateJson
import com.lasthopesoftware.resources.strings.guards.EncryptedString
import com.lasthopesoftware.resources.strings.guards.GuardStrings
import com.lasthopesoftware.resources.strings.parseJson
import com.namehillsoftware.handoff.promises.Promise
import kotlin.coroutines.cancellation.CancellationException

class LibrarySettingsAccess(
	private val libraryManager: ManageLibraries,
	private val jsonTranslator: TranslateJson,
	private val gsonTranslator: TranslateGson,
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

	override fun promiseSavedLibrarySettings(librarySettings: LibrarySettings): Promise<LibrarySettings> = Promise.Proxy { cs ->
		librarySettings.libraryId
			?.let(libraryManager::promiseLibrary)
			?.also(cs::doCancel)
			?.eventually { maybeLibrary ->
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
								val encryptionSettings = encryptedString?.run {
									StoredEncryptionSettings(
										initializationVector = initializationVector,
										password = protectedString,
										encryptionConfiguration = EncryptionConfiguration(this)
									)
								}

								ThreadPools.compute.preparePromise { cs ->
									if (cs.isCancelled) throw librarySettingsAccessCancelled()

									val jsonSettings = gsonTranslator.toJsonElement(connectionSettings).asJsonObject
									val encryptedJson =
										encryptionSettings?.let { gsonTranslator.toJsonElement(it) }?.asJsonObject?.asMap()
									if (encryptedJson != null) {
										jsonSettings.asMap().putAll(encryptedJson)
									}

									l.connectionSettings = gsonTranslator.serializeJson(jsonSettings)
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
					?.also(cs::doCancel)
					?.eventually(libraryManager::saveLibrary)
					?.also(cs::doCancel)
					?.eventually { it.promiseDecryptedLibrarySettings() }
					?.also(cs::doCancel)
					?: ThreadPools.compute.preparePromise { cs ->
						if (cs.isCancelled) throw librarySettingsAccessCancelled()
						librarySettings.toNewLibrary()
					}.also(cs::doCancel)
						.eventually(libraryManager::saveLibrary)
						.also(cs::doCancel)
						.then { it.toLibrarySettings() }
			}
			?: ThreadPools.compute.preparePromise { cs ->
				if (cs.isCancelled) throw librarySettingsAccessCancelled()
				librarySettings.toNewLibrary()
			}.also(cs::doCancel)
				.eventually(libraryManager::saveLibrary)
				.also(cs::doCancel)
				.then { it.toLibrarySettings() }
	}

	private fun LibrarySettings.toNewLibrary() = Library(
		libraryName = libraryName,
		isUsingExistingFiles = isUsingExistingFiles,
		serverType = when (connectionSettings) {
			is StoredMediaCenterConnectionSettings -> Library.ServerType.MediaCenter.name
			is StoredSubsonicConnectionSettings -> Library.ServerType.Subsonic.name
			null -> null
		},
		syncedFileLocation = syncedFileLocation,
		connectionSettings = connectionSettings?.let(jsonTranslator::serializeJson),
	)

	private fun Library.promiseDecryptedLibrarySettings(): Promise<LibrarySettings> =
		ThreadPools.compute.preparePromise { cs ->
			if (cs.isCancelled) throw librarySettingsAccessCancelled()

			toLibrarySettings() to connectionSettings?.let { jsonTranslator.parseJson<StoredEncryptionSettings>(it) }
		}
		.cancelBackEventually { (librarySettings, encryptionSettings) ->
			val connectionSettings = librarySettings.connectionSettings as? PasswordStoredConnectionSettings
			encryptionSettings
				?.takeUnless { it.password.isNullOrEmpty() }
				?.run {
					if (initializationVector == null || encryptionConfiguration == null) promiseSavedLibrarySettings(librarySettings)
					else promiseDecryptedPassword()
						.cancelBackThen { decryptedPassword, _ ->
							librarySettings.copy(
								connectionSettings = connectionSettings?.copy(password = decryptedPassword)
							)
						}
				}
				.keepPromise(librarySettings)
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
	private fun StoredEncryptionSettings.promiseDecryptedPassword(): Promise<String?> {
		val password = password ?: return Promise.empty()

		val encryptionConfiguration = encryptionConfiguration ?: return Promise.empty()
		val initializationVector = initializationVector ?: return Promise.empty()

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
