package com.lasthopesoftware.bluewater.client.browsing

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ViewAnimator
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesFragment
import com.lasthopesoftware.bluewater.client.browsing.items.list.IItemListViewContainer
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener.Companion.tryFlipToPreviousView
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistListFragment
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.ViewType
import com.lasthopesoftware.bluewater.client.browsing.library.views.*
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.CachedLibraryViewsProvider
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SelectedLibraryViewProvider
import com.lasthopesoftware.bluewater.client.browsing.library.views.adapters.SelectStaticViewAdapter
import com.lasthopesoftware.bluewater.client.browsing.library.views.adapters.SelectViewAdapter
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivityIntentBuilder
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsFragment
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils.buildStandardMenu
import com.lasthopesoftware.bluewater.shared.android.view.getValue
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.closables.lazyScoped
import com.lasthopesoftware.resources.intents.IntentFactory


class BrowserEntryActivity : AppCompatActivity(), IItemListViewContainer, Runnable {
	private val browseLibraryContainerRelativeLayout by LazyViewFinder<RelativeLayout>(this, R.id.browseLibraryContainer)
	private val selectViewsListView by LazyViewFinder<ListView>(this, R.id.lvLibraryViewSelection)
	private val specialLibraryItemsListView by LazyViewFinder<ListView>(this, R.id.specialLibraryItemsListView)
	private val drawerLayout by LazyViewFinder<DrawerLayout>(this, R.id.drawer_layout)
	private val loadingViewsProgressBar by LazyViewFinder<ProgressBar>(this, R.id.pbLoadingViews)

	private val libraryRepository by lazy { LibraryRepository(this)	}

	private val libraryViewsProvider by lazy { CachedLibraryViewsProvider.getInstance(this) }

	private val selectedLibraryViews by lazy {
		SelectedLibraryViewProvider(selectedBrowserLibraryProvider, libraryViewsProvider, libraryRepository)
	}

	private val selectedBrowserLibraryProvider by lazy { SelectedBrowserLibraryProvider(
		getCachedSelectedLibraryIdProvider(),
		libraryRepository)
	}

	private val messageHandler by lazy { Handler(mainLooper) }

	private val applicationMessageBus by lazyScoped { getApplicationMessageBus().getScopedMessageBus() }

	private val itemListMenuChangeHandler by lazy { ItemListMenuChangeHandler(this) }

	private val menuMessageBus by buildViewModelLazily { ViewModelMessageBus<ItemListMenuMessage>() }

	private val itemListMenuBackPressedHandler by lazyScoped { ItemListMenuBackPressedHandler(menuMessageBus) }

	private val applicationNavigation by lazy {
		ActivityApplicationNavigation(
			this,
			EditClientSettingsActivityIntentBuilder(IntentFactory(this)),
		)
	}

	private val specialViews by lazy {
		val views = arrayOf(
			SpecialView(
				ViewType.SearchView,
				getString(R.string.search),
				SearchViewItem,
				supportFragmentManager.fragments.firstOrNull { f -> f is SearchFilesFragment }
					?: SearchFilesFragment()
			),
			SpecialView(
				ViewType.DownloadView,
				getString(R.string.activeDownloads),
				DownloadViewItem,
				supportFragmentManager.fragments.firstOrNull { f -> f is ActiveFileDownloadsFragment }
					?: ActiveFileDownloadsFragment()
			),
		)

		val ft = supportFragmentManager.beginTransaction()
		try {
			for (view in views) {
				if (!supportFragmentManager.fragments.contains(view.fragment))
					ft.add(R.id.browseLibraryContainer, view.fragment)
				ft.hide(view.fragment)
			}
		} finally {
		    ft.commit()
		}

		views
	}

	private val drawerToggle = lazy {
		val selectViewTitle = getText(R.string.select_view_title)
		object : ActionBarDrawerToggle(
			this@BrowserEntryActivity,  /* host Activity */
			drawerLayout,  /* DrawerLayout object */
			R.string.drawer_open,  /* "open drawer" description */
			R.string.drawer_close /* "close drawer" description */
		) {
			/** Called when a drawer has settled in a completely closed state.  */
			override fun onDrawerClosed(view: View) {
				super.onDrawerClosed(view)
				supportActionBar?.title = oldTitle
				invalidateOptionsMenu() // creates resultFrom to onPrepareOptionsMenu()
			}

			/** Called when a drawer has settled in a completely open state.  */
			override fun onDrawerOpened(drawerView: View) {
				super.onDrawerOpened(drawerView)
				oldTitle = supportActionBar?.title
				supportActionBar?.title = selectViewTitle
				invalidateOptionsMenu() // creates resultFrom to onPrepareOptionsMenu()
			}
		}
	}

	private lateinit var nowPlayingFloatingActionButton: NowPlayingFloatingActionButton
	private var viewAnimator: ViewAnimator? = null
	private var activeFragment: Fragment? = null
	private var oldTitle = title
	private var isStopped = false

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.activity_browse_library)
		setSupportActionBar(findViewById(R.id.browseLibraryToolbar))
		setTheme(R.style.AppTheme)

		applicationMessageBus.registerReceiver(messageHandler) { _: SelectedConnectionSettingsChangeReceiver.SelectedConnectionSettingsUpdated ->
			startLibrary()
		}

		applicationMessageBus.registerReceiver(messageHandler) { _: BrowserLibrarySelection.LibraryChosenMessage ->
			finishAffinity()
		}

		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.browseLibraryRelativeLayout))

		setTitle(R.string.title_activity_library)

		supportActionBar?.apply {
			setDisplayHomeAsUpEnabled(true)
			setHomeButtonEnabled(true)
		}

		val drawerLayout = drawerLayout
		drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
		drawerLayout.addDrawerListener(drawerToggle.value)

		specialLibraryItemsListView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
			updateSelectedView(specialViews[position].viewType, position)
		}

		onBackPressedDispatcher.addCallback {
			if (!itemListMenuBackPressedHandler.hideAllMenus() && viewAnimator?.tryFlipToPreviousView() != false)
				finish()
		}
	}

	override fun onStart() {
		super.onStart()
		InstantiateSelectedConnectionActivity
			.restoreSelectedConnection(this)
			.eventually(response({ startLibrary() }, messageHandler))
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		if (showDownloadsAction == intent.action) updateSelectedView(ViewType.DownloadView, 0)
	}

	private fun startLibrary() {
		isStopped = false
		if (selectViewsListView.adapter != null) return

		showProgressBar()

		selectedBrowserLibraryProvider
			.browserLibrary
			.eventually(response({ library ->
				when {
					library == null -> {
						// No library, must bail out
						applicationNavigation.viewApplicationSettings()
						finish()
					}
					showDownloadsAction == intent.action -> {
						library.setSelectedView(0)
						library.setSelectedViewType(ViewType.DownloadView)
						libraryRepository.saveLibrary(library)
							.eventually(response(::displayLibrary, messageHandler))

						// Clear the action
						intent.action = null
					}
					else -> {
						displayLibrary(library)
					}
				}
			}, messageHandler))
			.excuse(HandleViewIoException(this, this))
			.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(this), this))
			.then {
				applicationNavigation.viewApplicationSettings()
				finish()
			}
	}

	private fun displayLibrary(library: Library?) {
		if (library == null) return

		specialLibraryItemsListView.adapter = SelectStaticViewAdapter(
			this,
			specialViews.map { it.name },
			library.selectedViewType,
			library.selectedView
		)

		run()
	}

	override fun run() {
		selectedLibraryViews
			.promiseSelectedOrDefaultView()
			.eventually { selectedView ->
				selectedView
					?.let { lv ->
						selectedBrowserLibraryProvider.browserLibrary
							.eventually { l ->
								l?.libraryId
									?.let { libraryId ->
										libraryViewsProvider
											.promiseLibraryViews(libraryId)
											.eventually(response({ items -> updateLibraryView(lv, items) }, messageHandler))
									}
									.keepPromise(Unit)
							}
					}
					.keepPromise(Unit)
			}
			.excuse(HandleViewIoException(this, this))
			.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(this), this))
			.then {
				applicationNavigation.viewApplicationSettings()
				finish()
			}
	}

	private fun updateLibraryView(selectedView: ViewItem, items: Collection<ViewItem>) {
		if (isStopped) return

		viewAnimator?.tryFlipToPreviousView()

		selectViewsListView.adapter = SelectViewAdapter(this, items, selectedView.key)
		selectViewsListView.onItemClickListener = getOnSelectViewClickListener(items)
		hideAllViews()

		val specialView = specialViews.firstOrNull { v -> v.viewItem == selectedView }
		if (specialView != null) {
			oldTitle = specialView.name
			supportActionBar?.title = oldTitle
			val fragment = specialView.fragment

			swapFragments(fragment)
			return
		}

		for (item in items) {
			if (item.key != selectedView.key) continue
			oldTitle = item.value
			supportActionBar?.title = oldTitle
			break
		}

		if (selectedView.value == KnownViews.Playlists) {
			val playlistListFragment = PlaylistListFragment()
			playlistListFragment.setOnItemListMenuChangeHandler(itemListMenuChangeHandler)
			swapFragments(playlistListFragment)
			return
		}

		val browseLibraryViewsFragment = BrowseLibraryViewsFragment()
		browseLibraryViewsFragment.setOnItemListMenuChangeHandler(itemListMenuChangeHandler)
		swapFragments(browseLibraryViewsFragment)
	}

	private fun getOnSelectViewClickListener(items: Collection<ViewItem>): OnItemClickListener {
		return OnItemClickListener { _, _, position, _ ->
			val selectedItem = (if (items is List<*>) items as List<ViewItem> else ArrayList(items))[position]
			updateSelectedView(
				if (KnownViews.Playlists == selectedItem.value) ViewType.PlaylistView
				else ViewType.StandardServerView, selectedItem.key)
		}
	}

	private fun updateSelectedView(selectedViewType: ViewType, selectedViewKey: Int) {
		drawerLayout.closeDrawer(GravityCompat.START)
		drawerToggle.value.syncState()

		selectedBrowserLibraryProvider
			.browserLibrary
			.then { library ->
				library
					?.takeUnless { selectedViewType === it.selectedViewType && it.selectedView == selectedViewKey }
					?.run {
						setSelectedView(selectedViewKey)
						setSelectedViewType(selectedViewType)
						libraryRepository
							.saveLibrary(this)
							.eventually(response(::displayLibrary, messageHandler))
					}
			}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean = buildStandardMenu(menu)

	override fun onOptionsItemSelected(item: MenuItem): Boolean =
		drawerToggle.isInitialized()
			&& drawerToggle.value.onOptionsItemSelected(item)
			|| ViewUtils.handleMenuClicks(this, item)

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		// Sync the toggle state after onRestoreInstanceState has occurred.
		if (drawerToggle.isInitialized()) drawerToggle.value.syncState()
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		if (drawerToggle.isInitialized()) drawerToggle.value.onConfigurationChanged(newConfig)
	}

	private fun showProgressBar() {
		showContainerView(loadingViewsProgressBar)
	}

	private fun showContainerView(view: View) {
		hideAllViews()
		view.visibility = View.VISIBLE
	}

	private fun hideAllViews() {
		for (i in 0 until browseLibraryContainerRelativeLayout.childCount)
			browseLibraryContainerRelativeLayout.getChildAt(i).visibility = View.INVISIBLE
	}

	@Synchronized
	private fun swapFragments(newFragment: Fragment) {
		val ft = supportFragmentManager.beginTransaction()
		try {
			if (specialViews.any { v -> v.fragment == activeFragment})
				activeFragment?.also(ft::hide)
			else
				activeFragment?.also(ft::remove)

			if (specialViews.any { v -> v.fragment == newFragment }) {
				ft.show(newFragment)
				return
			}

			ft.add(R.id.browseLibraryContainer, newFragment)
			ft.show(newFragment)
		} finally {
			ft.commit()
			activeFragment = newFragment
		}
	}

	public override fun onStop() {
		isStopped = true
		super.onStop()
	}

	override fun updateViewAnimator(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
	}

	override fun getNowPlayingFloatingActionButton(): NowPlayingFloatingActionButton = nowPlayingFloatingActionButton

	private class SpecialView(val viewType: ViewType, val name: String, val viewItem: ViewItem, val fragment: Fragment)

	companion object {
		val showDownloadsAction by lazy { MagicPropertyBuilder.buildMagicPropertyName<BrowserEntryActivity>("showDownloadsAction") }
	}
}
