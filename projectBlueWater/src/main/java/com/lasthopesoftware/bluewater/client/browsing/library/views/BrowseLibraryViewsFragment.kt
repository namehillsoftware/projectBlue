package com.lasthopesoftware.bluewater.client.browsing.library.views

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ViewAnimator
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener.Companion.tryFlipToPreviousView
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class BrowseLibraryViewsFragment : Fragment(R.layout.tabbed_library_items_layout), IItemListMenuChangeHandler, TabLayout.OnTabSelectedListener {

	companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<BrowseLibraryViewsFragment>()) }

		private val savedTabKey by lazy { magicPropertyBuilder.buildProperty("savedTabKey") }
		private val savedScrollPos by lazy { magicPropertyBuilder.buildProperty("savedScrollPos") }
		private val savedSelectedView by lazy { magicPropertyBuilder.buildProperty("SAVED_SELECTED_VIEW") }
	}

	private val lazyLibraryViewPagerAdapter = lazy {
		val viewChildPagerAdapter = LibraryViewPagerAdapter(childFragmentManager)
		viewChildPagerAdapter.setOnItemListMenuChangeHandler(this)
		viewChildPagerAdapter
	}

	private var viewAnimator: ViewAnimator? = null
	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null
	private lateinit var viewPager: ViewPager

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewPager = view.findViewById(R.id.libraryViewPager)

		val context = context ?: return

		val tabbedLibraryViewsContainer = view.findViewById<RelativeLayout>(R.id.tabbedLibraryViewsContainer)

		val libraryViewsTabs = view.findViewById<TabLayout>(R.id.tabsLibraryViews)
		libraryViewsTabs.addOnTabSelectedListener(this)

		val loadingView = view.findViewById<ProgressBar>(R.id.pbLoadingTabbedItems)

		CreateVisibleLibraryView(context, savedInstanceState, libraryViewsTabs, loadingView, tabbedLibraryViewsContainer)
	}

	fun setOnItemListMenuChangeHandler(itemListMenuChangeHandler: IItemListMenuChangeHandler?) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler
	}

	override fun onAllMenusHidden() {
		itemListMenuChangeHandler?.onAllMenusHidden()
	}

	override fun onAnyMenuShown() {
		itemListMenuChangeHandler?.onAnyMenuShown()
	}

	override fun onViewChanged(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
		itemListMenuChangeHandler?.onViewChanged(viewAnimator)
	}

	override fun onTabSelected(tab: TabLayout.Tab?) {}

	override fun onTabUnselected(tab: TabLayout.Tab?) {
		viewAnimator?.tryFlipToPreviousView()
	}

	override fun onTabReselected(tab: TabLayout.Tab?) {}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		val viewPager = viewPager
		outState.putInt(savedTabKey, viewPager.currentItem)
		outState.putInt(savedScrollPos, viewPager.scrollY)
		selectedBrowserLibrary
			.then { library -> if (library != null) outState.putInt(savedSelectedView, library.selectedView) }
	}

	private val selectedBrowserLibrary: Promise<Library?>
		get() {
			val context = context ?: return Promise.empty()
			val applicationSettingsRepository = context.getApplicationSettingsRepository()
			val selectedLibraryIdentifierProvider = SelectedBrowserLibraryIdentifierProvider(applicationSettingsRepository)
			val selectedBrowserLibraryProvider = SelectedBrowserLibraryProvider(selectedLibraryIdentifierProvider, LibraryRepository(context))
			return selectedBrowserLibraryProvider.browserLibrary
		}

	private inner class CreateVisibleLibraryView(
		private val context: Context,
		private val savedInstanceState: Bundle?,
		private val libraryViewsTabs: TabLayout,
		private val loadingView: ProgressBar,
		private val tabbedLibraryViewsContainer: RelativeLayout
	) : Runnable, ImmediateResponse<List<Item>, Unit> {

		private val handler = lazy { Handler(context.mainLooper) }
		private val fillVisibleViews = lazy { LoopedInPromise.response(this, handler.value) }

		init {
			tabbedLibraryViewsContainer.visibility = View.INVISIBLE
			loadingView.visibility = View.VISIBLE
			run()
		}

		override fun run() {
			selectedBrowserLibrary
				.then { it?.let { library ->
					CachedItemProvider.getInstance(context)
						.promiseItems(library.libraryId, ItemId(library.selectedView))
						.eventually(fillVisibleViews.value)
						.run {
							if (savedInstanceState == null) this
							else eventually(LoopedInPromise.response({
								val savedSelectedView = savedInstanceState.getInt(savedSelectedView, -1)
								if (savedSelectedView < 0 || savedSelectedView != library.selectedView) return@response

								val savedTabKey = savedInstanceState.getInt(savedTabKey, -1)
								if (savedTabKey > -1) viewPager.currentItem = savedTabKey

								val savedScrollPosition = savedInstanceState.getInt(savedScrollPos, -1)
								if (savedScrollPosition > -1) viewPager.scrollY = savedScrollPosition
							}, handler.value))
						}
						.excuse(HandleViewIoException(context, this))
						.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(context), handler.value))
					}
				}
		}

		override fun respond(result: List<Item>) {
			val viewChildPagerAdapter = lazyLibraryViewPagerAdapter.value
			viewChildPagerAdapter.setLibraryViews(result)

			// Set up the ViewPager with the sections adapter.
			viewPager.adapter = viewChildPagerAdapter
			libraryViewsTabs.setupWithViewPager(viewPager)
			libraryViewsTabs.visibility = if (result.size <= 1) View.GONE else View.VISIBLE
			loadingView.visibility = View.INVISIBLE
			tabbedLibraryViewsContainer.visibility = View.VISIBLE
		}
	}
}
