package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider.Companion.provide
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
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.tutorials.TutorialManager
import com.namehillsoftware.handoff.promises.Promise

class ItemListFragment : Fragment() {
	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null
	private val listView by lazy {
		val listView = ListView(activity)
		listView.visibility = View.INVISIBLE
		listView
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

	private val layout by lazy {
		val layout = RelativeLayout(activity)
		layout.layoutParams = RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT
		)
		layout.addView(progressBar)
		layout.addView(listView)
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

	private val tutorialManager by lazy { TutorialManager(requireContext()) }

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = layout

	override fun onStart() {
		super.onStart()
		val activity = activity ?: return
		listView.visibility = View.INVISIBLE
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
					}, activity)

				object : Runnable {
					override fun run() {
						getInstance(activity).promiseSessionConnection()
							.eventually { c -> c?.let { provide(c, activeLibrary.selectedView) } ?: Promise(emptyList()) }
							.eventually(onGetVisibleViewsCompleteListener)
							.excuse(HandleViewIoException(activity, this))
							.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(activity), activity))
					}
				}.run()
			}
	}

	private fun fillStandardItemView(category: IItem) {
		val activity = activity ?: return

		lazySelectedLibraryProvider
			.browserLibrary
			.then { library ->
				if (library == null || context == null) return@then
				val itemListMenuChangeHandler = itemListMenuChangeHandler ?: return@then

				val onGetLibraryViewItemResultsComplete =
					response(
						OnGetLibraryViewItemResultsComplete(
							activity,
							listView,
							progressBar,
							itemListMenuChangeHandler,
							FileListParameters.getInstance(),
							fileStringListProvider,
							StoredItemAccess(activity),
							library,
							tutorialManager
						), activity
					)
				val fillItemsRunnable = object : Runnable {
					override fun run() {
						getInstance(activity).promiseSessionConnection()
							.eventually { c -> c?.let { provide(c, category.key) }.keepPromise() }
							.eventually(onGetLibraryViewItemResultsComplete)
							.excuse(HandleViewIoException(activity, this))
							.eventuallyExcuse(
								response(
									UnexpectedExceptionToasterResponse(activity),
									activity
								)
							)
					}
				}
				fillItemsRunnable.run()
			}
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
}
