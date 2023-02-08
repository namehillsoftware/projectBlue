package com.lasthopesoftware.bluewater.client.stored.library.items.files.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.RateControlledFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.SelectedLibraryFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.SelectedLibraryUrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.resources.strings.StringResources

class ActiveFileDownloadsFragment : Fragment() {

	private val applicationMessageBus by lazy { getApplicationMessageBus() }

	private val selectedLibraryIdProvider by lazy { requireContext().getCachedSelectedLibraryIdProvider() }

	private val libraryConnectionProvider by lazy { ConnectionSessionManager.get(requireContext()) }

	private val scopedUrlKeyProvider by lazy {
		SelectedLibraryUrlKeyProvider(
			selectedLibraryIdProvider,
			UrlKeyProvider(libraryConnectionProvider)
		)
	}

	private val revisionProvider by lazy { LibraryRevisionProvider(libraryConnectionProvider) }

	private val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			libraryConnectionProvider,
			FilePropertyCache,
			RateControlledFilePropertiesProvider(
				FilePropertiesProvider(
					libraryConnectionProvider,
					revisionProvider,
					FilePropertyCache,
				),
				PromisingRateLimiter(1),
			),
		)
	}

	private val scopedFilePropertiesProvider by lazy {
		SelectedLibraryFilePropertiesProvider(
			selectedLibraryIdProvider,
			libraryFilePropertiesProvider,
		)
	}

	private val reusableFileItemViewModelProvider by lazy {
		ReusableFileItemViewModelProvider(
			scopedFilePropertiesProvider,
			scopedUrlKeyProvider,
			StringResources(requireContext()),
			applicationMessageBus
		)
	}

	private val viewModel by buildViewModelLazily {
		ActiveFileDownloadsViewModel(
			StoredFileAccess(requireContext()),
			applicationMessageBus,
			SyncScheduler(requireContext())
		)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
		ComposeView(requireContext()).apply {
			setContent {
				ProjectBlueTheme {
					ActiveFileDownloadsView(
						viewModel,
						reusableFileItemViewModelProvider
					)
				}
			}
		}

	override fun onAttach(context: Context) {
		super.onAttach(context)

		selectedLibraryIdProvider
			.promiseSelectedLibraryId()
			.then {
				if (it != null)
					viewModel.loadActiveDownloads(it)
			}
	}
}
