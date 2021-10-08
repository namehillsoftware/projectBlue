package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.DemoableItemListAdapter
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.views.handlers.OnGetLibraryViewItemResultsComplete
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.tutorials.TutorialManager

class PlaylistListFragment : Fragment() {
    private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null

	private val recyclerView by lazy {
		val recyclerView = RecyclerView(requireActivity())
		recyclerView.visibility = View.INVISIBLE
		recyclerView
	}

	private val progressBar by lazy {
		val pbLoading = ProgressBar(activity, null, android.R.attr.progressBarStyleLarge)
		val pbParams = RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
		)
		pbParams.addRule(RelativeLayout.CENTER_IN_PARENT)
		pbLoading.layoutParams = pbParams
		pbLoading
	}

    private val lazyLayout by lazy {
		val activity: Activity? = activity
		val layout = RelativeLayout(activity)
		layout.layoutParams = RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT
		)
		layout.addView(progressBar)
		layout.addView(recyclerView)
		layout
	}

	private val fileStringListProvider by lazy {
		FileStringListProvider(SelectedConnectionProvider(requireContext()))
	}

	private val selectedLibraryProvider by lazy {
		SelectedBrowserLibraryProvider(
			SelectedBrowserLibraryIdentifierProvider(requireContext().getApplicationSettingsRepository()),
			LibraryRepository(requireContext()))
	}

	private val tutorialManager by lazy { TutorialManager(requireContext()) }

	private val lazySelectedLibraryProvider by lazy {
		SelectedBrowserLibraryProvider(
			SelectedBrowserLibraryIdentifierProvider(requireContext().getApplicationSettingsRepository()),
			LibraryRepository(requireContext())
		)
	}

	private val promisedBrowserLibrary by lazy { lazySelectedLibraryProvider.browserLibrary }

	private val promisedItemProvider by lazy {
		getInstance(requireContext())
			.promiseSessionConnection()
			.then { c -> c?.let(::ItemProvider) }
	}

	private val messageBus by lazy { MessageBus(LocalBroadcastManager.getInstance(requireContext())) }

	private val demoableItemListAdapter by lazy {
		promisedItemProvider.eventually { itemProvider ->
			itemProvider
				?.let {
					promisedBrowserLibrary.then {
						it?.let { library ->
							itemListMenuChangeHandler?.let { itemListMenuChangeHandler ->
								val activity = requireActivity()

								DemoableItemListAdapter(
									activity,
									messageBus,
									FileListParameters.getInstance(),
									fileStringListProvider,
									itemListMenuChangeHandler,
									StoredItemAccess(activity),
									itemProvider,
									library,
									tutorialManager
								)
							}
						}
					}
				}
				.keepPromise()
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = lazyLayout

    override fun onStart() {
        super.onStart()

		val activity = activity ?: return

        recyclerView.visibility = View.INVISIBLE
        progressBar.visibility = View.VISIBLE

		promisedBrowserLibrary.then { library ->
			library?.also {
				demoableItemListAdapter
					.then { adapter ->
						adapter
							?.let {
								response(
									OnGetLibraryViewItemResultsComplete(
										it,
										recyclerView,
										progressBar,
									), activity
								)
							}
							?.also { listResolvedPromise ->
								object : Runnable {
									override fun run() {
										promisedItemProvider
											.eventually { i ->
												i?.promiseItems(library.selectedView).keepPromise(emptyList())
											}
											.eventually(listResolvedPromise)
											.excuse(HandleViewIoException(activity, this))
											.eventuallyExcuse(
												response(
													UnexpectedExceptionToasterResponse(activity),
													activity
												)
											)
									}
								}.run()
							}
					}
			}
		}
	}

    fun setOnItemListMenuChangeHandler(itemListMenuChangeHandler: IItemListMenuChangeHandler?) {
        this.itemListMenuChangeHandler = itemListMenuChangeHandler
    }
}
