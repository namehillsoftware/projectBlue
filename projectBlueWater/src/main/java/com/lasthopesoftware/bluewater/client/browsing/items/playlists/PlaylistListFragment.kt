package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.DemoableItemListAdapter
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListAdapter
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.ActivityLaunching
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.ScopedApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import com.lasthopesoftware.bluewater.tutorials.TutorialManager

class PlaylistListFragment : Fragment() {
    private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null

	private val handler by lazy { Handler(requireContext().mainLooper) }

	private val tutorialManager by lazy { TutorialManager(requireContext()) }

	private val browserLibraryIdProvider by lazy {
		SelectedBrowserLibraryIdentifierProvider(requireContext().getApplicationSettingsRepository())
	}

	private val promisedBrowserLibrary by lazy {
		SelectedBrowserLibraryProvider(
			browserLibraryIdProvider,
			LibraryRepository(requireContext())
		).browserLibrary
	}

	private val itemProvider by lazy { CachedItemProvider.getInstance(requireContext()) }

	private val itemListProvider by lazy {
		val connectionProvider = ConnectionSessionManager.get(requireContext())

		ItemStringListProvider(
            FileListParameters,
            LibraryFileStringListProvider(connectionProvider)
        )
	}

	private val applicationMessageBus = lazy {
		val messageBus = requireContext().getApplicationMessageBus()
		ScopedApplicationMessageBus(messageBus, messageBus).apply {
			registerReceiver { l: ActivityLaunching ->
				val isLaunching = l != ActivityLaunching.HALTED

				recyclerView?.visibility = ViewUtils.getVisibility(!isLaunching)
				progressBar?.visibility = ViewUtils.getVisibility(isLaunching)
			}
		}
	}

	private val demoableItemListAdapter by lazy {
		browserLibraryIdProvider.selectedLibraryId.then {
			it?.let { libraryId ->
				itemListMenuChangeHandler?.let { itemListMenuChangeHandler ->
					activity
						?.let { fa ->
							DemoableItemListAdapter(
								fa,
								applicationMessageBus.value,
								itemListProvider,
								itemListMenuChangeHandler,
								StoredItemAccess(fa),
								itemProvider,
								libraryId,
								tutorialManager
							)
						}
						?: requireContext()
							.let { context ->
								ItemListAdapter(
									context,
									applicationMessageBus.value,
									itemListProvider,
									itemListMenuChangeHandler,
									StoredItemAccess(context),
									itemProvider,
									libraryId,
								)
							}
				}
			}
		}
	}

	private var recyclerView: RecyclerView? = null
	private var progressBar: ProgressBar? = null
	private var layout: RelativeLayout? = null

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		layout = inflater.inflate(R.layout.asynchronous_recycler_view, container, false) as RelativeLayout
		progressBar = layout?.findViewById(R.id.recyclerLoadingProgress)
		recyclerView = layout?.findViewById(R.id.loadedRecyclerView)
		return layout
	}

	override fun onStart() {
		super.onStart()

        recyclerView?.visibility = ViewUtils.getVisibility(false)
        progressBar?.visibility = ViewUtils.getVisibility(true)

		promisedBrowserLibrary.then { library ->
			library?.also {
				demoableItemListAdapter
					.eventually(response({ adapter ->
						recyclerView
							?.takeIf { it.adapter == null || it.adapter != adapter }
							?.also {
								it.adapter = adapter
								val layoutManager = LinearLayoutManager(context)
								it.layoutManager = layoutManager
								it.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))
							}

						adapter?.also { ItemHydration(library, adapter) }
					}, handler))
			}
		}
	}

    fun setOnItemListMenuChangeHandler(itemListMenuChangeHandler: IItemListMenuChangeHandler?) {
        this.itemListMenuChangeHandler = itemListMenuChangeHandler
    }

	override fun onDestroy() {
		super.onDestroy()

		if (applicationMessageBus.isInitialized())
			applicationMessageBus.value.close()
	}

	private inner class ItemHydration(private val library: Library, private val adapter: ItemListAdapter) : Runnable {
		init {
		    run()
		}

		override fun run() {
			val context = requireContext()
			itemProvider.promiseItems(library.libraryId, ItemId(library.selectedView))
				.eventually { i -> i?.let(adapter::updateListEventually) }
				.eventually(response({
					progressBar?.visibility = ViewUtils.getVisibility(false)
					recyclerView?.visibility = ViewUtils.getVisibility(true)
				}, handler))
				.excuse(HandleViewIoException(context, this))
				.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(context), handler))
		}
	}
}
