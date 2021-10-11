package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
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
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.MenuNotifications
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider
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

class ItemListFragment : Fragment() {
	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null

	private val handler by lazy { Handler(requireContext().mainLooper) }

	private val fileStringListProvider by lazy {
		FileStringListProvider(SelectedConnectionProvider(requireContext()))
	}

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

	private val tutorialManager by lazy { TutorialManager(requireContext()) }

	private val messageBus = lazy {
		MessageBus(LocalBroadcastManager.getInstance(requireContext())).apply {

			val intentFilter = IntentFilter()
			intentFilter.addAction(MenuNotifications.launchingActivity)
			intentFilter.addAction(MenuNotifications.launchingActivityFinished)
			registerReceiver(
				object : BroadcastReceiver() {
					override fun onReceive(context: Context?, intent: Intent?) {
						val isLaunching = intent?.action == MenuNotifications.launchingActivity

						recyclerView?.visibility = ViewUtils.getVisibility(!isLaunching)
						progressBar?.visibility = ViewUtils.getVisibility(isLaunching)
					}
				},
				intentFilter
			)
		}
	}

	private val demoableItemListAdapter by lazy {
		promisedItemProvider.eventually { itemProvider ->
			itemProvider
				?.let {
					promisedBrowserLibrary.then {
						it?.let { library ->
							itemListMenuChangeHandler?.let { itemListMenuChangeHandler ->
								activity
									?.let { fa ->
										DemoableItemListAdapter(
											fa,
											messageBus.value,
											FileListParameters.getInstance(),
											fileStringListProvider,
											itemListMenuChangeHandler,
											StoredItemAccess(fa),
											itemProvider,
											library,
											tutorialManager
										)
									}
									?: requireContext()
										.let { context ->
											ItemListAdapter(
												context,
												messageBus.value,
												FileListParameters.getInstance(),
												fileStringListProvider,
												itemListMenuChangeHandler,
												StoredItemAccess(context),
												itemProvider,
												library,
											)
										}
							}
						}
					}
				}
				.keepPromise()
		}
	}

	private var recyclerView: RecyclerView? = null
	private var progressBar: ProgressBar? = null
	private var layout: RelativeLayout? = null

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		layout = inflater.inflate(R.layout.layout_list_view, container) as RelativeLayout
		progressBar = layout?.findViewById(R.id.recyclerLoadingProgress)
		recyclerView = layout?.findViewById(R.id.loadedRecyclerView)
		return layout
	}

	override fun onStart() {
		super.onStart()

		val context = requireContext()

		recyclerView?.visibility = View.GONE
		progressBar?.visibility = View.VISIBLE

		lazySelectedLibraryProvider
			.browserLibrary
			.then { activeLibrary ->
				if (activeLibrary == null) return@then

				val onGetVisibleViewsCompleteListener =
					response<List<Item>, Unit>({ result ->
						result
							.takeUnless { result.isEmpty() }
							?.let {
								arguments
									?.getInt(ARG_CATEGORY_POSITION)
									?.let { categoryPosition ->
										if (categoryPosition < result.size) result[categoryPosition]
										else result[result.size - 1]
									}
							}
							?.also(::fillStandardItemView)
					}, context)

				object : Runnable {
					override fun run() {
						promisedItemProvider
							.eventually { i -> i?.promiseItems(activeLibrary.selectedView).keepPromise(emptyList()) }
							.eventually(onGetVisibleViewsCompleteListener)
							.excuse(HandleViewIoException(context, this))
							.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(context), handler))
					}
				}.run()
			}
	}

	override fun onDestroy() {
		super.onDestroy()

		if (messageBus.isInitialized())
			messageBus.value.clear()
	}

	private fun fillStandardItemView(category: IItem) {
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
				adapter?.also { ItemHydration(category, adapter) }
			}, handler))
	}

	fun setOnItemListMenuChangeHandler(itemListMenuChangeHandler: IItemListMenuChangeHandler?) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler
	}

	companion object {
		private const val ARG_CATEGORY_POSITION = "category_position"

		@JvmStatic
		fun getPreparedFragment(libraryViewId: Int): ItemListFragment {
			val returnFragment = ItemListFragment()
			val args = Bundle()
			args.putInt(ARG_CATEGORY_POSITION, libraryViewId)
			returnFragment.arguments = args
			return returnFragment
		}
	}

	private inner class ItemHydration(private val category: IItem, private val adapter: ItemListAdapter) : Runnable {
		init {
			run()
		}

		override fun run() {
			val context = requireContext()
			promisedItemProvider
				.eventually { i ->
					i?.promiseItems(category.key).keepPromise(emptyList())
				}
				.eventually { i -> i?.let(adapter::updateListEventually).keepPromise(Unit) }
				.eventually(response({
					progressBar?.visibility = ViewUtils.getVisibility(false)
					recyclerView?.visibility = ViewUtils.getVisibility(true)
				}, handler))
				.excuse(HandleViewIoException(context, this))
				.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(context), handler))
		}
	}
}
