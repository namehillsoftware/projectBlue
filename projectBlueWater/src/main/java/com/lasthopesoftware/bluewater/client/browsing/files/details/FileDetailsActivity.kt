package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.SelectedLibraryFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.SelectedLibraryFilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.StaticLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.authentication.SelectedLibraryConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.SelectedLibraryUrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.initialization.*
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.getIntent
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.resources.strings.StringResources

class FileDetailsActivity : ComponentActivity() {

	companion object {

		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<FileDetailsActivity>()) }
		val playlist by lazy { magicPropertyBuilder.buildProperty("playlist") }
		val playlistPosition by lazy { magicPropertyBuilder.buildProperty("playlistPosition") }
		val libraryIdKey by lazy { magicPropertyBuilder.buildProperty("libraryId") }

		fun Context.launchFileDetailsActivity(libraryId: LibraryId, playlist: Collection<ServiceFile>, position: Int) {
			startActivity(getIntent<FileDetailsActivity>().apply {
				putExtra(playlistPosition, position)
				putExtra(Companion.playlist, playlist.map { it.key }.toIntArray())
				putExtra(libraryIdKey, libraryId)
			})
		}
	}

	private val imageProvider by lazy { CachedImageProvider.getInstance(this) }

	private val defaultImageProvider by lazy { DefaultImageProvider(this) }

	private val selectedLibraryIdProvider by lazy { StaticLibraryIdentifierProvider(getCachedSelectedLibraryIdProvider()) }

	private val libraryConnections by lazy { buildNewConnectionSessionManager() }

	private val libraryRevisionProvider by lazy { LibraryRevisionProvider(libraryConnections) }

	private val filePropertiesProvider by lazy {
		EditableScopedFilePropertiesProvider(
			SelectedLibraryFilePropertiesProvider(
				selectedLibraryIdProvider,
				FilePropertiesProvider(
					libraryConnections,
					libraryRevisionProvider,
					FilePropertyCache,
				),
			)
		)
	}

	private val scopedFilePropertyUpdates by lazy {
		SelectedLibraryFilePropertyStorage(
			selectedLibraryIdProvider,
			FilePropertyStorage(
				libraryConnections,
				ConnectionAuthenticationChecker(libraryConnections),
				libraryRevisionProvider,
				FilePropertyCache,
				ApplicationMessageBus.getApplicationMessageBus(),
			)
		)
	}

	private val connectionPermissions by lazy {
		SelectedLibraryConnectionAuthenticationChecker(
			selectedLibraryIdProvider,
			ConnectionAuthenticationChecker(libraryConnections)
		)
	}

	private val vm by buildViewModelLazily {
		FileDetailsViewModel(
			connectionPermissions,
			filePropertiesProvider,
			scopedFilePropertyUpdates,
			defaultImageProvider,
			imageProvider,
			PlaybackServiceController(this),
			ApplicationMessageBus.getApplicationMessageBus(),
			SelectedLibraryUrlKeyProvider(selectedLibraryIdProvider, UrlKeyProvider(libraryConnections)),
		)
	}

	private val connectionStatusViewModel by lazy {
		val applicationNavigation = ActivityApplicationNavigation(this, IntentBuilder(this))

		ConnectionStatusViewModel(
			StringResources(this),
			ConnectionInitializationErrorController(
				DramaticConnectionInitializationController(
					ConnectionInitializationProxy(libraryConnections),
					libraryConnections,
				),
				applicationNavigation,
			),
		)
	}

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			ProjectBlueTheme {
				val isGettingConnection by connectionStatusViewModel.isGettingConnection.collectAsState()

				if (isGettingConnection) {
					ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
				} else {
					FileDetailsView(vm)
				}
			}
		}

		val libraryId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getParcelableExtra(libraryIdKey, cls<LibraryId>())
		} else {
			intent.getParcelableExtra(libraryIdKey)
		}

		if (libraryId == null) {
			finish()
			return
		}

		connectionStatusViewModel.ensureConnectionIsWorking(libraryId).eventually(LoopedInPromise.response({
			val position = intent.getIntExtra(playlistPosition, -1)
			val playlist = intent.getIntArrayExtra(playlist)?.map(::ServiceFile) ?: emptyList()
			setView(playlist, position)
		}, this))
	}

	private fun setView(playlist: List<ServiceFile>, position: Int) {
		if (position < 0) {
			finish()
			return
		}

		vm.loadFromList(playlist, position)
			.excuse(HandleViewIoException(this) { setView(playlist, position) })
			.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(this), this))
			.then { finish() }
	}
}

