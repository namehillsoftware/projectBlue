package com.lasthopesoftware.bluewater.client.browsing

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.FileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
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
import com.namehillsoftware.handoff.promises.Promise
import kotlin.math.max

class ExternalBrowserService : MediaBrowserServiceCompat() {
	companion object {
		private val rateLimiter by lazy { PromisingRateLimiter<Map<String, String>>(max(Runtime.getRuntime().availableProcessors() - 1, 1)) }

		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(ExternalBrowserService::class.java) }
		private val rootBrowserId by lazy { magicPropertyBuilder.buildProperty("rootBrowserId") }
		private val rejectionBrowserId by lazy { magicPropertyBuilder.buildProperty("rejectionBrowserId") }
		val error by lazy { magicPropertyBuilder.buildProperty("error") }

		private fun toMediaItem(item: Item): MediaBrowserCompat.MediaItem =
			MediaBrowserCompat.MediaItem(
				MediaDescriptionCompat
					.Builder()
					.setMediaId(item.key.toString())
					.setTitle(item.value)
					.build(),
				MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
			)
	}

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

	override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
		return BrowserRoot(rootBrowserId, Bundle.EMPTY)
	}

	override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
		if (parentId == rejectionBrowserId) {
			result.sendResult(ArrayList())
			return
		}

		browserLibraryIdProvider
			.selectedLibraryId
			.eventually { maybeId ->
				maybeId
					?.let { libraryId ->
						parentId
							.toIntOrNull()
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

	private fun promiseMediaItem(serviceFile: ServiceFile): Promise<MediaBrowserCompat.MediaItem> =
		filePropertiesProvider.promiseFileProperties(serviceFile)
			.then { p ->
				MediaBrowserCompat.MediaItem(
					MediaDescriptionCompat
						.Builder()
						.setMediaId(serviceFile.key.toString())
						.setTitle(p[KnownFileProperties.NAME])
						.build(),
					MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
				)
			}
}
