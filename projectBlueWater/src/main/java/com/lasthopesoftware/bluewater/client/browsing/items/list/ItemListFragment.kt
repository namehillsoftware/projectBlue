package com.lasthopesoftware.bluewater.client.browsing.items.list

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
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.ActivityLaunching
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.stored.library.items.StateChangeBroadcastingStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.ScopedApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.tutorials.TutorialManager

class ItemListFragment : Fragment() {
	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null

	private val handler by lazy { Handler(requireContext().mainLooper) }

	private val browserLibraryIdProvider by lazy {
		SelectedBrowserLibraryIdentifierProvider(requireContext().getApplicationSettingsRepository())
	}

	private val selectedLibraryProvider by lazy {
		SelectedBrowserLibraryProvider(
			browserLibraryIdProvider,
			LibraryRepository(requireContext())
		)
	}

	private val itemListProvider by lazy {
		val connectionProvider = ConnectionSessionManager.get(requireContext())

		ItemStringListProvider(
            FileListParameters,
            LibraryFileStringListProvider(connectionProvider)
        )
	}

	private val itemProvider by lazy { CachedItemProvider.getInstance(requireContext()) }

	private val tutorialManager by lazy { TutorialManager(requireContext()) }

	private val applicationMessageBus = lazy {
		val applicationMessageBus = requireContext().getApplicationMessageBus()
		ScopedApplicationMessageBus(applicationMessageBus, applicationMessageBus).apply {
			registerReceiver { l: ActivityLaunching ->
				val isLaunching = l != ActivityLaunching.HALTED // Only show the item list view again when launching error'ed for some reason

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
									StateChangeBroadcastingStoredItemAccess(StoredItemAccess(context), applicationMessageBus.value),
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
		progressBar = layout?.findViewById(R.id.items_loading_progress)
		recyclerView = layout?.findViewById(R.id.loaded_recycler_view)
		return layout
	}

	override fun onStart() {
		super.onStart()

		val context = requireContext()

		recyclerView?.visibility = ViewUtils.getVisibility(false)
		progressBar?.visibility = ViewUtils.getVisibility(true)

		selectedLibraryProvider
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
							?.also { fillStandardItemView(activeLibrary.libraryId, it) }
					}, context)

				object : Runnable {
					override fun run() {
						itemProvider.promiseItems(activeLibrary.libraryId, ItemId(activeLibrary.selectedView))
							.eventually(onGetVisibleViewsCompleteListener)
							.excuse(HandleViewIoException(context, this))
							.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(context), handler))
					}
				}.run()
			}
	}

	override fun onDestroy() {
		super.onDestroy()

		if (applicationMessageBus.isInitialized())
			applicationMessageBus.value.close()
	}

	private fun fillStandardItemView(libraryId: LibraryId, category: IItem) {
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

				adapter?.also { ItemHydration(libraryId, category, adapter) }
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

	private inner class ItemHydration(private val libraryId: LibraryId, private val category: IItem, private val adapter: ItemListAdapter) : Runnable {
		init {
			run()
		}

		override fun run() {
			val context = requireContext()
			itemProvider.promiseItems(libraryId, ItemId(category.key))
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
