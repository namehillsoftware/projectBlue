package com.lasthopesoftware.bluewater.client.stored.library.items.files.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.adapter.ActiveFileDownloadsAdapter
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.databinding.LayoutDownloadsBinding
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ActiveFileDownloadsFragment : Fragment() {

	private val applicationMessageBus by lazy { getApplicationMessageBus() }

	private val viewModel by buildViewModelLazily {
		ActiveFileDownloadsViewModel(
			StoredFileAccess(requireContext()),
			applicationMessageBus,
			SyncScheduler(requireContext())
		)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		if (container == null) return null

		val binding = DataBindingUtil.inflate<LayoutDownloadsBinding>(inflater, R.layout.layout_downloads, container, false)
		binding.vm = viewModel
		val context = container.context
		val activeFileDownloadsAdapter = ActiveFileDownloadsAdapter(context)
		val listView = binding.downloadsList.loadedRecyclerView
		listView.adapter = activeFileDownloadsAdapter
		viewModel.downloadingFiles.onEach {
			activeFileDownloadsAdapter.updateListEventually(it.values.toList()).suspend()
		}.launchIn(lifecycleScope)

		binding.toggleSyncButton.setOnClickListener { viewModel.toggleSync() }

		return binding.root
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)

		SelectedLibraryIdProvider(context.getApplicationSettingsRepository())
			.promiseSelectedLibraryId()
			.then {
				if (it != null)
					viewModel.loadActiveDownloads(it)
			}
	}
}
