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
import androidx.appcompat.view.ContextThemeWrapper
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

	private val recyclerView by lazy {
		val context = requireContext()
		val recyclerView = RecyclerView(ContextThemeWrapper(context, R.style.VerticalScrollbarRecyclerView))
		recyclerView.visibility = View.INVISIBLE

		demoableItemListAdapter.eventually(response({ adapter ->
			recyclerView.adapter = adapter
			val layoutManager = LinearLayoutManager(context)
			recyclerView.layoutManager = layoutManager
			recyclerView.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))
		}, handler))

		recyclerView
	}

	private val progressBar by lazy {
		val pbLoading = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleLarge)
		val pbParams = RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
		)
		pbParams.addRule(RelativeLayout.CENTER_IN_PARENT)
		pbLoading.layoutParams = pbParams
		pbLoading
	}

	private val layout by lazy {
		val layout = RelativeLayout(requireContext())
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

	private val messageBus = lazy { MessageBus(LocalBroadcastManager.getInstance(requireContext())) }

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

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
		layout.apply { requestLayout() }

	override fun onStart() {
		super.onStart()

		val context = requireContext()

		val intentFilter = IntentFilter()
		intentFilter.addAction(MenuNotifications.launchingActivity)
		intentFilter.addAction(MenuNotifications.launchingActivityFinished)
		messageBus.value.registerReceiver(
			object : BroadcastReceiver() {
				override fun onReceive(context: Context?, intent: Intent?) {
					val isLaunching = intent?.action == MenuNotifications.launchingActivity

					recyclerView.visibility = ViewUtils.getVisibility(!isLaunching)
					progressBar.visibility = ViewUtils.getVisibility(isLaunching)
				}
			},
			intentFilter
		)

		recyclerView.visibility = View.INVISIBLE
		progressBar.visibility = View.VISIBLE

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
			.then { adapter -> adapter?.also { ItemHydration(category, adapter) } }
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
					progressBar.visibility = ViewUtils.getVisibility(false)
					recyclerView.visibility = ViewUtils.getVisibility(true)
				}, handler))
				.excuse(HandleViewIoException(context, this))
				.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(context), handler))
		}
	}
}
