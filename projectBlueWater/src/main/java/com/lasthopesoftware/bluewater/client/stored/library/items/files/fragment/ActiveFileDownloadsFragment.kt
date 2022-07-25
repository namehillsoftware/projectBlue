package com.lasthopesoftware.bluewater.client.stored.library.items.files.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.fragment.adapter.ActiveFileDownloadsAdapter
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileMessage
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.client.stored.sync.SyncStateMessage
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class ActiveFileDownloadsFragment : Fragment() {
	private var onSyncStartedReceiver: ((SyncStateMessage.SyncStarted) -> Unit)? = null
	private var onSyncStoppedReceiver: ((SyncStateMessage.SyncStopped) -> Unit)? = null
	private var onFileQueuedReceiver: ((StoredFileMessage.FileQueued) -> Unit)? = null
	private var onFileDownloadedReceiver: ((StoredFileMessage.FileDownloaded) -> Unit)? = null

	private val applicationMessageBus = lazy { requireContext().getApplicationMessageBus() }

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		if (container == null) return null

		val viewFilesLayout = inflater.inflate(R.layout.layout_downloads, container, false) as RelativeLayout
		val progressBar = viewFilesLayout.findViewById<ProgressBar>(R.id.items_loading_progress)
		val listView = viewFilesLayout.findViewById<RecyclerView>(R.id.loaded_recycler_view)
		listView.visibility = View.INVISIBLE
		progressBar.visibility = View.VISIBLE

		val context = container.context
		val activeFileDownloadsAdapter = ActiveFileDownloadsAdapter(context)
		listView.adapter = activeFileDownloadsAdapter
		val layoutManager = LinearLayoutManager(context)
		listView.layoutManager = layoutManager

		listView.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))

		val applicationSettingsRepository = context.getApplicationSettingsRepository()
		val libraryRepository = LibraryRepository(context)
		val selectedBrowserLibraryProvider = SelectedBrowserLibraryProvider(
			SelectedBrowserLibraryIdentifierProvider(applicationSettingsRepository),
			libraryRepository)

		selectedBrowserLibraryProvider
			.browserLibrary
			.then { library ->
				val storedFileAccess = StoredFileAccess(
                    context
                )

				storedFileAccess.downloadingStoredFiles
					.eventually(LoopedInPromise.response({ storedFiles ->
						val localStoredFiles = ConcurrentHashMap(storedFiles.groupBy { sf -> sf.id }.values.associate { sf -> Pair(sf.first().id, sf.first()) })

						activeFileDownloadsAdapter.updateListEventually(localStoredFiles.values.toList())

						onFileDownloadedReceiver?.apply(applicationMessageBus.value::unregisterReceiver)
						applicationMessageBus.value
							.registerReceiver({ message: StoredFileMessage.FileDownloaded ->
								localStoredFiles.remove(message.storedFileId)
								activeFileDownloadsAdapter.updateListEventually(localStoredFiles.values.toList())
								Unit
							}.also { onFileDownloadedReceiver = it })

						onFileQueuedReceiver?.also(applicationMessageBus.value::unregisterReceiver)
						applicationMessageBus.value.registerReceiver({ message: StoredFileMessage.FileQueued ->
							message.storedFileId
								.takeUnless(localStoredFiles::containsKey)
								?.also { storedFileId ->
									storedFileAccess
										.getStoredFile(storedFileId)
										.eventually { storedFile ->
											if (storedFile != null && storedFile.libraryId == library?.id) {
												localStoredFiles[storedFileId] = storedFile
												activeFileDownloadsAdapter.updateListEventually(localStoredFiles.values.toList())
											} else {
												Promise.empty()
											}
										}
								}

							Unit
						}.also { onFileQueuedReceiver = it })

						progressBar.visibility = View.INVISIBLE
						listView.visibility = View.VISIBLE
					}, context))
			}

		val toggleSyncButton = viewFilesLayout.findViewById<Button>(R.id.toggleSyncButton)
		val startSyncLabel = context.getText(R.string.start_sync_button)
		val stopSyncLabel = context.getText(R.string.stop_sync_button)
		toggleSyncButton.isEnabled = false
		SyncScheduler.promiseIsSyncing(context).eventually(LoopedInPromise.response({ isRunning ->
			toggleSyncButton.text = if (!isRunning) startSyncLabel else stopSyncLabel
			toggleSyncButton.isEnabled = true
		}, context))

		onSyncStartedReceiver?.also(applicationMessageBus.value::unregisterReceiver)
		applicationMessageBus.value
			.registerReceiver({ _ : SyncStateMessage.SyncStarted -> toggleSyncButton.text = stopSyncLabel }.also { onSyncStartedReceiver = it })

		onSyncStoppedReceiver?.also(applicationMessageBus.value::unregisterReceiver)
		applicationMessageBus.value
			.registerReceiver({ _ : SyncStateMessage.SyncStopped -> toggleSyncButton.text = startSyncLabel }.also { onSyncStoppedReceiver = it })

		toggleSyncButton.setOnClickListener { v ->
			SyncScheduler.promiseIsSyncing(v.context).then { isSyncRunning ->
				if (isSyncRunning) SyncScheduler.cancelSync(v.context)
				else SyncScheduler.syncImmediately(context)
			}
		}
		return viewFilesLayout
	}

	override fun onDestroy() {
		super.onDestroy()

		if (applicationMessageBus.isInitialized()) {
			with (applicationMessageBus.value) {
				onSyncStartedReceiver?.also(::unregisterReceiver)
				onSyncStoppedReceiver?.also(::unregisterReceiver)
				onFileDownloadedReceiver?.also(::unregisterReceiver)
				onFileQueuedReceiver?.also(::unregisterReceiver)
			}
		}
	}
}
