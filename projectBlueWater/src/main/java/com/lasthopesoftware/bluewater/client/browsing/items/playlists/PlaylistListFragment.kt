package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.DemoableItemListAdapter
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.MenuNotifications
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.tutorials.TutorialManager

class PlaylistListFragment : Fragment() {
    private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null

	private val recyclerView by lazy {
		val activity = requireActivity()
		val recyclerView = RecyclerView(activity)
		recyclerView.visibility = View.INVISIBLE

		demoableItemListAdapter.eventually(response({ adapter ->
			recyclerView.adapter = adapter
			val layoutManager = LinearLayoutManager(activity)
			recyclerView.layoutManager = layoutManager
			recyclerView.addItemDecoration(DividerItemDecoration(activity, layoutManager.orientation))
		}, activity))

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

	private val tutorialManager by lazy { TutorialManager(requireContext()) }

	private val promisedBrowserLibrary by lazy {
		SelectedBrowserLibraryProvider(
			SelectedBrowserLibraryIdentifierProvider(requireContext().getApplicationSettingsRepository()),
			LibraryRepository(requireContext())
		).browserLibrary
	}

	private val promisedItemProvider by lazy {
		getInstance(requireContext())
			.promiseSessionConnection()
			.then { c -> c?.let(::ItemProvider) }
	}

	private val messageBus = lazy { MessageBus(LocalBroadcastManager.getInstance(requireContext())) }

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
									messageBus.value,
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

	private val launchingActivityReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			val isLaunching = intent?.action == MenuNotifications.launchingActivity

			recyclerView.visibility = ViewUtils.getVisibility(!isLaunching)
			progressBar.visibility = ViewUtils.getVisibility(isLaunching)
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = lazyLayout

    override fun onStart() {
        super.onStart()

		val activity = activity ?: return

		val intentFilter = IntentFilter()
		intentFilter.addAction(MenuNotifications.launchingActivity)
		intentFilter.addAction(MenuNotifications.launchingActivityFinished)
		messageBus.value.registerReceiver(
			launchingActivityReceiver,
			intentFilter
		)

        recyclerView.visibility = View.INVISIBLE
        progressBar.visibility = View.VISIBLE

		promisedBrowserLibrary.then { library ->
			library?.also {
				demoableItemListAdapter
					.then { adapter -> adapter?.also { ItemHydration(activity, library, adapter) } }
			}
		}
	}

    fun setOnItemListMenuChangeHandler(itemListMenuChangeHandler: IItemListMenuChangeHandler?) {
        this.itemListMenuChangeHandler = itemListMenuChangeHandler
    }

	override fun onDestroy() {
		super.onDestroy()

		if (messageBus.isInitialized())
			messageBus.value.unregisterReceiver(launchingActivityReceiver)
	}

	private inner class ItemHydration(private val activity: Activity, private val library: Library, private val adapter: DemoableItemListAdapter) : Runnable {
		init {
		    run()
		}

		override fun run() {
			promisedItemProvider
				.eventually { i ->
					i?.promiseItems(library.selectedView).keepPromise(emptyList())
				}
				.eventually { i -> i?.let(adapter::updateListEventually) }
				.eventually(response({
					progressBar.visibility = ViewUtils.getVisibility(false)
					recyclerView.visibility = ViewUtils.getVisibility(true)
				}, activity))
				.excuse(HandleViewIoException(activity, this))
				.eventuallyExcuse(
					response(
						UnexpectedExceptionToasterResponse(activity),
						activity
					)
				)
		}
	}
}
