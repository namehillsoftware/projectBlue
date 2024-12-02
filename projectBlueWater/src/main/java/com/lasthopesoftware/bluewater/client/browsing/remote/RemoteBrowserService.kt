package com.lasthopesoftware.bluewater.client.browsing.remote

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.CachedItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.DelegatingFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.InMemoryNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.MediaSession.MediaSessionService
import com.lasthopesoftware.bluewater.shared.android.services.promiseBoundService
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.policies.retries.CloseableRetryOnRejectionLazyPromise
import com.lasthopesoftware.policies.retries.RateLimitingExecutionPolicy
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.getSafely
import com.lasthopesoftware.promises.toFuture
import com.lasthopesoftware.resources.PackageValidator
import kotlin.math.max

private val logger by lazyLogger<RemoteBrowserService>()

class RemoteBrowserService : MediaBrowserServiceCompat() {
	companion object {
		// Potentially useful magic android strings (see https://github.com/android/uamp/blob/99e44c1c5106218c62eff552b64bbc12f1883a22/common/src/main/java/com/example/android/uamp/media/MusicService.kt)
		private const val mediaSearchSupported = "android.media.browse.SEARCH_SUPPORTED"
		private const val contentStyleBrowsableHint = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
		private const val contentStylePlayableHint = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
		private const val contentStyleSupport = "android.media.browse.CONTENT_STYLE_SUPPORTED"
		private const val contentStyleList = 1
		private const val contentStyleGrid = 2
		const val serviceFileMediaIdPrefix = "sf"
		const val itemFileMediaIdPrefix = "it"
		private const val playlistFileMediaIdPrefix = "pl"
		const val mediaIdDelimiter = ':'

		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<RemoteBrowserService>()) }

		private val root by lazy { magicPropertyBuilder.buildProperty("root") }
		private val recentRoot by lazy { magicPropertyBuilder.buildProperty("recentRoot") }
		private val rejection by lazy { magicPropertyBuilder.buildProperty("rejection") }
		val error by lazy { magicPropertyBuilder.buildProperty("error") }
	}

	private val packageValidator by lazy { PackageValidator(this, R.xml.allowed_media_browser_callers) }

	private val itemProvider by lazy { CachedItemProvider.getInstance(this) }

	private val libraryFileStringListProvider by lazy { LibraryFileStringListProvider(ConnectionSessionManager.get(this)) }

	private val fileProvider by lazy {
		LibraryFileProvider(libraryFileStringListProvider)
	}

	private val itemFileProvider by lazy { CachedItemFileProvider.getInstance(this) }

	private val selectedLibraryIdProvider by lazy { getCachedSelectedLibraryIdProvider() }

	private val rateLimitingPolicy by lazy { RateLimitingExecutionPolicy(max(Runtime.getRuntime().availableProcessors() - 1, 1)) }

	private val filePropertiesProvider by lazy {
		val libraryConnectionProvider = buildNewConnectionSessionManager()
		CachedFilePropertiesProvider(
			libraryConnectionProvider,
			FilePropertyCache,
			DelegatingFilePropertiesProvider(
				FilePropertiesProvider(
					GuaranteedLibraryConnectionProvider(libraryConnectionProvider),
					LibraryRevisionProvider(libraryConnectionProvider),
					FilePropertyCache,
				),
				rateLimitingPolicy,
			)
		)
	}

	private val imageProvider by lazy { CachedImageProvider.getInstance(this) }

	private val mediaItemServiceFileLookup by lazy {
		MediaItemServiceFileLookup(
			filePropertiesProvider,
			imageProvider,
		)
	}

	private val nowPlayingMediaItemLookup by lazy {
		val libraryRepository = LibraryRepository(this)

		val repository = NowPlayingRepository(
			selectedLibraryIdProvider,
			libraryRepository,
			libraryRepository,
			InMemoryNowPlayingState,
		)

		NowPlayingMediaItemLookup(
			selectedLibraryIdProvider,
			repository,
			mediaItemServiceFileLookup
		)
	}

	private val mediaItemBrowser by lazy {
		MediaItemsBrowser(
			selectedLibraryIdProvider,
			itemProvider,
			fileProvider,
			itemFileProvider,
			mediaItemServiceFileLookup
		)
	}

	private val lazyMediaSessionService = CloseableRetryOnRejectionLazyPromise { promiseBoundService<MediaSessionService>() }

	override fun onCreate() {
		super.onCreate()
		lazyMediaSessionService.value.then { s ->
			sessionToken = s.service.mediaSession.sessionToken
		}
	}

	override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? =
		if (!packageValidator.isKnownCaller(clientPackageName, clientUid)) null
		else rootHints
			?.takeIf { it.getBoolean(BrowserRoot.EXTRA_RECENT) }
			?.let { Bundle() }
			?.apply { putBoolean(BrowserRoot.EXTRA_RECENT, true) }
			// Return a tree with a single playable media item for resumption.
			?.let { extras -> BrowserRoot(recentRoot, extras) }
			?: Bundle()
				.apply {
					putBoolean(mediaSearchSupported, true)
					putBoolean(contentStyleSupport, true)
					putInt(contentStyleBrowsableHint, contentStyleGrid)
					putInt(contentStylePlayableHint, contentStyleList)
				}
				.let { bundle ->
					BrowserRoot(root, bundle)
				}

	override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
		if (parentId == rejection) {
			result.sendResult(ArrayList())
			return
		}

		result.detach()

		if (parentId == recentRoot) {
			nowPlayingMediaItemLookup
				.promiseNowPlayingItem().keepPromise()
				.then { it -> it?.let { mutableListOf(it) }.apply(result::sendResult) }
				.excuse { e -> result.sendError(Bundle().apply { putString(error, e.message) }) }
			return
		}

		val promisedMediaItems = parentId
			.takeIf { id -> id.startsWith(itemFileMediaIdPrefix) }
			?.substring(3)
			?.toIntOrNull()
			?.let { id ->
				mediaItemBrowser.promiseItems(ItemId(id)).keepPromise(emptyList())
			}
			?: mediaItemBrowser.promiseLibraryItems().keepPromise(emptyList())

		promisedMediaItems
			.then { items -> result.sendResult(items.toMutableList()) }
			.excuse { e -> result.sendError(Bundle().apply { putString(error, e.message) }) }
	}

	override fun onLoadItem(itemId: String?, result: Result<MediaBrowserCompat.MediaItem>) {
		val itemIdParts = itemId?.split(mediaIdDelimiter, limit = 2)
		if (itemIdParts == null || itemIdParts.size < 2) return super.onLoadItem(itemId, result)

		val type = itemIdParts[0]
		if (type != serviceFileMediaIdPrefix) return super.onLoadItem(itemId, result)

		val id = itemIdParts[1].toIntOrNull() ?: return super.onLoadItem(itemId, result)

		result.detach()

		selectedLibraryIdProvider
			.promiseSelectedLibraryId()
			.eventually {
				it?.let { l ->
					mediaItemServiceFileLookup.promiseMediaItemWithImage(l, ServiceFile(id))
				}.keepPromise()
			}
			.then(result::sendResult)
			.excuse { e -> result.sendError(Bundle().apply { putString(error, e.message) }) }
	}

	override fun onSearch(query: String, extras: Bundle?, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
		result.detach()
		mediaItemBrowser.promiseItems(query)
			.then { items -> result.sendResult(items.toMutableList()) }
			.excuse { e -> result.sendError(Bundle().apply { putString(error, e.message) }) }
	}

	override fun onDestroy() {
		try {
			lazyMediaSessionService.promiseClose().toFuture().getSafely()
		} catch (e: Throwable) {
			logger.error("There was an error destroying the service", e)
		}
		super.onDestroy()
	}
}
