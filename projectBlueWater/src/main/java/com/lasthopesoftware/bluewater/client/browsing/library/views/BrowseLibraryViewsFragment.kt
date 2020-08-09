package com.lasthopesoftware.bluewater.client.browsing.library.views

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ViewAnimator
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.astuetz.PagerSlidingTabStrip
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise

class BrowseLibraryViewsFragment : Fragment(R.layout.tabbed_library_items_layout), IItemListMenuChangeHandler {

	companion object {
		private val SAVED_TAB_KEY = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment::class.java, "SAVED_TAB_KEY")
		private val SAVED_SCROLL_POS = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment::class.java, "SAVED_SCROLL_POS")
		private val SAVED_SELECTED_VIEW = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment::class.java, "SAVED_SELECTED_VIEW")
	}

	private var viewAnimator: ViewAnimator? = null
	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null
	private var viewPager: ViewPager? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewPager = view.findViewById(R.id.libraryViewPager)

		val context = context ?: return

		val tabbedLibraryViewsContainer = view.findViewById<RelativeLayout>(R.id.tabbedLibraryViewsContainer)
		val libraryViewsTabs = view.findViewById<PagerSlidingTabStrip>(R.id.tabsLibraryViews)

		libraryViewsTabs.setOnPageChangeListener(object : OnPageChangeListener {
			override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
			override fun onPageSelected(position: Int) {
				LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)
			}
			override fun onPageScrollStateChanged(state: Int) {}
		})

		val loadingView = view.findViewById<ProgressBar>(R.id.pbLoadingTabbedItems)
		tabbedLibraryViewsContainer.visibility = View.INVISIBLE
		loadingView.visibility = View.VISIBLE

		val handler = Handler(context.mainLooper)

		val onGetVisibleViewsCompleteListener = LoopedInPromise.response({ result: List<Item> ->
			val viewChildPagerAdapter = LibraryViewPagerAdapter(childFragmentManager)
			viewChildPagerAdapter.setOnItemListMenuChangeHandler(this@BrowseLibraryViewsFragment)
			viewChildPagerAdapter.setLibraryViews(result)

			// Set up the ViewPager with the sections adapter.
			viewPager?.adapter = viewChildPagerAdapter
			libraryViewsTabs.setViewPager(viewPager)
			libraryViewsTabs.visibility = if (result.size <= 1) View.GONE else View.VISIBLE
			loadingView.visibility = View.INVISIBLE
			tabbedLibraryViewsContainer.visibility = View.VISIBLE
		}, handler)

		val fillItemsAction = object : Runnable {
			override fun run() {
				selectedBrowserLibrary
					.then { it?.let { library ->
						getInstance(context).promiseSessionConnection()
							.eventually { c -> ItemProvider.provide(c, library.selectedView) }
							.eventually(onGetVisibleViewsCompleteListener)
							.eventually<Unit>(LoopedInPromise.response({
								val savedSelectedView = savedInstanceState?.getInt(SAVED_SELECTED_VIEW, -1) ?: -1
								if (savedSelectedView < 0 || savedSelectedView != library.selectedView) return@response

								val savedTabKey = savedInstanceState?.getInt(SAVED_TAB_KEY, -1) ?: -1
								if (savedTabKey > -1) viewPager?.currentItem = savedTabKey

								val savedScrollPosition = savedInstanceState?.getInt(SAVED_SCROLL_POS, -1) ?: -1
								if (savedScrollPosition > -1) viewPager?.scrollY = savedScrollPosition
							}, handler))
							.excuse(HandleViewIoException(context, this))
							.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(context), handler))
					}
				}
			}
		}
		fillItemsAction.run()
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

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		val viewPager = viewPager ?: return
		outState.putInt(SAVED_TAB_KEY, viewPager.currentItem)
		outState.putInt(SAVED_SCROLL_POS, viewPager.scrollY)
		selectedBrowserLibrary
			.then { library -> if (library != null) outState.putInt(SAVED_SELECTED_VIEW, library.selectedView) }
	}

	private val selectedBrowserLibrary: Promise<Library?>
		get() {
			val context = context ?: return Promise.empty()
			val selectedLibraryIdentifierProvider = SelectedBrowserLibraryIdentifierProvider(context)
			val libraryProvider = LibraryRepository(context)
			return libraryProvider.getLibrary(selectedLibraryIdentifierProvider.selectedLibraryId)
		}
}
