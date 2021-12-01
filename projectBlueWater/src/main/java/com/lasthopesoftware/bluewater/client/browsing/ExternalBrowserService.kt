package com.lasthopesoftware.bluewater.client.browsing

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.FileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.SearchFileParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.*
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.CachedLibraryViewsProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.PackageValidator
import com.namehillsoftware.handoff.promises.Promise
import kotlin.math.max

class ExternalBrowserService : MediaBrowserServiceCompat() {
	companion object {
		// Potentially useful magic android strings (see https://github.com/android/uamp/blob/99e44c1c5106218c62eff552b64bbc12f1883a22/common/src/main/java/com/example/android/uamp/media/MusicService.kt)
		private const val mediaSearchSupported = "android.media.browse.SEARCH_SUPPORTED"
		private const val contentStyleBrowsableHint = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
		private const val contentStylePlayableHint = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
		private const val contentStyleSupport = "android.media.browse.CONTENT_STYLE_SUPPORTED"
		private const val serviceFileMediaIdPrefix = "sf:"
		private const val itemFileMediaIdPrefix = "it:"
		private const val playlistFileMediaIdPrefix = "pl:"

		private val rateLimiter by lazy { PromisingRateLimiter<Map<String, String>>(max(Runtime.getRuntime().availableProcessors() - 1, 1)) }

		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(ExternalBrowserService::class.java) }

		private val root by lazy { magicPropertyBuilder.buildProperty("root") }
		private val rejection by lazy { magicPropertyBuilder.buildProperty("rejection") }
		val error by lazy { magicPropertyBuilder.buildProperty("error") }

		private fun toMediaItem(item: Item): MediaBrowserCompat.MediaItem =
			MediaBrowserCompat.MediaItem(
				MediaDescriptionCompat
					.Builder()
					.setMediaId(itemFileMediaIdPrefix + item.key)
					.setTitle(item.value)
					.build(),
				MediaBrowserCompat.MediaItem.FLAG_BROWSABLE or MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
			)
	}

	private val packageValidator by lazy { PackageValidator(this, R.xml.allowed_media_browser_callers) }

	private val browserLibraryIdProvider by lazy { SelectedBrowserLibraryIdentifierProvider(getApplicationSettingsRepository()) }

	private val libraryViewsProvider by lazy { CachedLibraryViewsProvider.getInstance(this) }

	private val itemProvider by lazy { CachedItemProvider.getInstance(this) }

	private val fileProvider by lazy {
		val stringListProvider = FileStringListProvider(SelectedConnectionProvider(this))
		FileProvider(stringListProvider)
	}

	private val filePropertiesProvider by lazy {
		SelectedConnectionFilePropertiesProvider(SelectedConnectionProvider(this)) { c ->
			val filePropertyCache = FilePropertyCache.getInstance()
			ScopedCachedFilePropertiesProvider(
				c,
				filePropertyCache,
				RateControlledFilePropertiesProvider(
					ScopedFilePropertiesProvider(
						c,
						ScopedRevisionProvider(c),
						filePropertyCache
					),
					rateLimiter
				)
			)
		}
	}

	override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
		if (!packageValidator.isKnownCaller(clientPackageName, clientUid)) return null

		val bundle = Bundle().apply {
			putBoolean(mediaSearchSupported, true)
		}
		return BrowserRoot(root, bundle)
	}

	override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
		if (parentId == rejection) {
			result.sendResult(ArrayList())
			return
		}

		result.detach()
		browserLibraryIdProvider
			.selectedLibraryId
			.eventually { maybeId ->
				maybeId
					?.let { libraryId ->
						parentId
							.takeIf { id -> id.startsWith(itemFileMediaIdPrefix) }
							?.substring(3)
							?.toIntOrNull()
							?.let { id ->
								itemProvider
									.promiseItems(libraryId, id)
									.eventually { items ->
										if (items.any()) items.map(::toMediaItem).toPromise()
										else {
											val parameters = FileListParameters.getInstance().getFileListParameters(Item(id))
											fileProvider
												.promiseFiles(FileListParameters.Options.None, *parameters)
												.eventually { files -> Promise.whenAll(files.map(::promiseMediaItem)) }
										}
									}
							}
							?: libraryViewsProvider.promiseLibraryViews(libraryId).then { v -> v.map(::toMediaItem) }
					}
					?: Promise(emptyList())
			}
			.then { items -> result.sendResult(items.toMutableList()) }
			.excuse { e -> result.sendError(Bundle().apply { putString(error, e.message) }) }
	}

	override fun onLoadItem(itemId: String?, result: Result<MediaBrowserCompat.MediaItem>) {
		val itemIdParts = itemId?.split(':')
		if (itemIdParts == null || itemIdParts.size < 2) {
			result.sendResult(null)
			return
		}
		val type = itemIdParts[0]
		val id = itemIdParts[1].toIntOrNull()
		if (id == null || type != serviceFileMediaIdPrefix) {
			result.sendResult(null)
			return
		}

		result.detach()

		promiseMediaItem(ServiceFile(id))
			.then(result::sendResult)
			.excuse { e -> result.sendError(Bundle().apply { putString(error, e.message) }) }
	}

	override fun onSearch(query: String, extras: Bundle?, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
		result.detach()
		val parameters = SearchFileParameterProvider.getFileListParameters(query)
		fileProvider
			.promiseFiles(FileListParameters.Options.None, *parameters)
			.eventually { files -> Promise.whenAll(files.map(::promiseMediaItem)) }
			.then { items -> result.sendResult(items.toMutableList()) }
			.excuse { e -> result.sendError(Bundle().apply { putString(error, e.message) }) }
	}

	private fun promiseMediaItem(serviceFile: ServiceFile): Promise<MediaBrowserCompat.MediaItem> =
		filePropertiesProvider.promiseFileProperties(serviceFile)
			.then { p ->
				MediaBrowserCompat.MediaItem(
					MediaDescriptionCompat
						.Builder()
						.setMediaId(serviceFileMediaIdPrefix + p[KnownFileProperties.KEY])
						.setTitle(p[KnownFileProperties.NAME])
						.build(),
					MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
				)
			}
}
