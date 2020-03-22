package com.lasthopesoftware.bluewater.client.browsing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ViewAnimator
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.browsing.items.list.IItemListViewContainer
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistListFragment
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.ViewType
import com.lasthopesoftware.bluewater.client.browsing.library.views.*
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.*
import com.lasthopesoftware.bluewater.client.browsing.library.views.adapters.SelectStaticViewAdapter
import com.lasthopesoftware.bluewater.client.browsing.library.views.adapters.SelectViewAdapter
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity
import com.lasthopesoftware.bluewater.client.connection.session.ProvideSessionConnection
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnectionProvider
import com.lasthopesoftware.bluewater.client.servers.selection.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.servers.selection.LibrarySelectionKey
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.fragment.ActiveFileDownloadsFragment
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import com.namehillsoftware.lazyj.CreateAndHold
import com.namehillsoftware.lazyj.Lazy
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Callable

class BrowserEntryActivity : AppCompatActivity(), IItemListViewContainer, Runnable {
	/**
	 * The [ViewPager] that will host the section contents.
	 */
	private val browseLibraryContainerRelativeLayout = LazyViewFinder<RelativeLayout>(this, R.id.browseLibraryContainer)
	private val selectViewsListView = LazyViewFinder<ListView>(this, R.id.lvLibraryViewSelection)
	private val specialLibraryItemsListView = LazyViewFinder<ListView>(this, R.id.specialLibraryItemsListView)
	private val drawerLayout = LazyViewFinder<DrawerLayout>(this, R.id.drawer_layout)
	private val loadingViewsProgressBar = LazyViewFinder<ProgressBar>(this, R.id.pbLoadingViews)
	private val lazyLibraryRepository: CreateAndHold<LibraryRepository> = object : AbstractSynchronousLazy<LibraryRepository>() {
		override fun create(): LibraryRepository {
			return LibraryRepository(this@BrowserEntryActivity)
		}
	}
	private val drawerToggle: CreateAndHold<ActionBarDrawerToggle> = object : AbstractSynchronousLazy<ActionBarDrawerToggle>() {
		override fun create(): ActionBarDrawerToggle {
			val selectViewTitle = getText(R.string.select_view_title)
			return object : ActionBarDrawerToggle(
				this@BrowserEntryActivity,  /* host Activity */
				drawerLayout.findView(),  /* DrawerLayout object */
				R.string.drawer_open,  /* "open drawer" description */
				R.string.drawer_close /* "close drawer" description */
			) {
				/** Called when a drawer has settled in a completely closed state.  */
				override fun onDrawerClosed(view: View) {
					super.onDrawerClosed(view)
					supportActionBar!!.title = oldTitle
					invalidateOptionsMenu() // creates resultFrom to onPrepareOptionsMenu()
				}

				/** Called when a drawer has settled in a completely open state.  */
				override fun onDrawerOpened(drawerView: View) {
					super.onDrawerOpened(drawerView)
					oldTitle = supportActionBar!!.title
					supportActionBar!!.title = selectViewTitle
					invalidateOptionsMenu() // creates resultFrom to onPrepareOptionsMenu()
				}
			}
		}
	}
	private val lazySelectedBrowserLibraryProvider: CreateAndHold<ISelectedBrowserLibraryProvider> = object : AbstractSynchronousLazy<ISelectedBrowserLibraryProvider>() {
		override fun create(): ISelectedBrowserLibraryProvider {
			return SelectedBrowserLibraryProvider(
				SelectedBrowserLibraryIdentifierProvider(this@BrowserEntryActivity),
				lazyLibraryRepository.getObject())
		}
	}
	private val libraryChosenEventReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val chosenLibrary = intent.getIntExtra(LibrarySelectionKey.chosenLibraryKey, -1)
			if (chosenLibrary >= 0) finishAffinity()
		}
	}
	private val lazyLocalBroadcastManager: CreateAndHold<LocalBroadcastManager> = Lazy(Callable { LocalBroadcastManager.getInstance(this) })
	private val lazySessionConnectionProvider: CreateAndHold<ProvideSessionConnection> = Lazy(Callable { SessionConnectionProvider(this) })
	private val lazyLibraryViewsProvider: CreateAndHold<ProvideLibraryViews> = object : AbstractSynchronousLazy<ProvideLibraryViews>() {
		override fun create(): ProvideLibraryViews {
			return LibraryViewsProvider(
				lazySessionConnectionProvider.getObject(),
				LibraryViewsByConnectionProvider())
		}
	}
	private val lazySelectedLibraryViews: CreateAndHold<ProvideSelectedLibraryView> = object : AbstractSynchronousLazy<ProvideSelectedLibraryView>() {
		override fun create(): ProvideSelectedLibraryView {
			return SelectedLibraryViewProvider(
				lazySelectedBrowserLibraryProvider.getObject(),
				lazyLibraryViewsProvider.getObject(),
				lazyLibraryRepository.getObject())
		}
	}
	private var viewAnimator: ViewAnimator? = null
	private var nowPlayingFloatingActionButton: NowPlayingFloatingActionButton? = null
	private var activeFragment: Fragment? = null
	private var oldTitle = title
	private var isStopped = false
	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Ensure that this task is only started when it's the task root. A workaround for an Android bug.
		// See http://stackoverflow.com/a/7748416
		if (!isTaskRoot) {
			val intent = intent
			if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
				val className = BrowserEntryActivity::class.java.name
				LoggerFactory.getLogger(javaClass).info("$className is not the root.  Finishing $className instead of launching.")
				finish()
				return
			}
		}

		setContentView(R.layout.activity_browse_library)

		lazyLocalBroadcastManager.getObject().registerReceiver(
			libraryChosenEventReceiver,
			IntentFilter(BrowserLibrarySelection.libraryChosenEvent))

		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.browseLibraryRelativeLayout))

		setTitle(R.string.title_activity_library)
		supportActionBar!!.setDisplayHomeAsUpEnabled(true)
		supportActionBar!!.setHomeButtonEnabled(true)

		val drawerLayout = drawerLayout.findView()
		drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
		drawerLayout.addDrawerListener(drawerToggle.getObject())

		specialLibraryItemsListView.findView().onItemClickListener = OnItemClickListener { _, _, _, _ -> updateSelectedView(ViewType.DownloadView, 0) }
	}

	public override fun onResume() {
		super.onResume()
		InstantiateSessionConnectionActivity.restoreSessionConnection(this)
			.eventually(LoopedInPromise.response({ restore -> if (!restore!!) startLibrary() }, this))
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) startLibrary()
		super.onActivityResult(requestCode, resultCode, data)
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		if (showDownloadsAction == intent.action) updateSelectedView(ViewType.DownloadView, 0)
	}

	private fun startLibrary() {
		isStopped = false
		if (selectViewsListView.findView().adapter != null) return
		showProgressBar()
		lazySelectedBrowserLibraryProvider.getObject()
			.browserLibrary
			.eventually(LoopedInPromise.response({ library ->
				when {
					library == null -> {
						// No library, must bail out
						finish()
					}
					showDownloadsAction == intent.action -> {
						library.setSelectedView(0)
						library.setSelectedViewType(ViewType.DownloadView)
						lazyLibraryRepository.getObject().saveLibrary(library)
							.eventually(LoopedInPromise.response({ displayLibrary(it) }, this))

						// Clear the action
						intent.action = null
					}
					else -> {
						displayLibrary(library)
					}
				}
			}, this))
	}

	private fun displayLibrary(library: Library) {
		specialLibraryItemsListView.findView().adapter = SelectStaticViewAdapter(this, specialViews, library.selectedViewType, library.selectedView)
		run()
	}

	override fun run() {
		lazySelectedLibraryViews.getObject()
			.promiseSelectedOrDefaultView()
			.eventually { selectedView ->
				lazyLibraryViewsProvider.getObject().promiseLibraryViews()
					.eventually(LoopedInPromise.response(
						{ items: Collection<ViewItem>? -> updateLibraryView(selectedView!!, items) },
						this))
			}
			.excuse(HandleViewIoException(this, this))
			.excuse(ForwardedResponse.forward())
			.eventually(LoopedInPromise.response(UnexpectedExceptionToasterResponse(this), this))
			.then {
				ApplicationSettingsActivity.launch(this)
				finish()
			}
	}

	private fun updateLibraryView(selectedView: ViewItem, items: Collection<ViewItem>?) {
		if (isStopped || items == null) return

		LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)

		selectViewsListView.findView().adapter = SelectViewAdapter(this, items, selectedView.key)
		selectViewsListView.findView().onItemClickListener = getOnSelectViewClickListener(items)
		hideAllViews()

		if (selectedView is DownloadViewItem) {
			oldTitle = specialViews[0]
			supportActionBar!!.title = oldTitle
			val activeFileDownloadsFragment = ActiveFileDownloadsFragment()
			swapFragments(activeFileDownloadsFragment)
			return
		}

		for (item in items) {
			if (item.key != selectedView.key) continue
			oldTitle = item.value
			supportActionBar!!.title = oldTitle
			break
		}

		if (selectedView is PlaylistViewItem) {
			val playlistListFragment = PlaylistListFragment()
			playlistListFragment.setOnItemListMenuChangeHandler(ItemListMenuChangeHandler(this))
			swapFragments(playlistListFragment)
			return
		}

		val browseLibraryViewsFragment = BrowseLibraryViewsFragment()
		browseLibraryViewsFragment.setOnItemListMenuChangeHandler(ItemListMenuChangeHandler(this))
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
		drawerLayout.findView().closeDrawer(GravityCompat.START)
		drawerToggle.getObject().syncState()

		lazySelectedBrowserLibraryProvider.getObject()
			.browserLibrary
			.then(ImmediateResponse { library ->
				if (selectedViewType === library.selectedViewType && library.selectedView == selectedViewKey)
					return@ImmediateResponse

				library.setSelectedView(selectedViewKey)
				library.setSelectedViewType(selectedViewType)
				lazyLibraryRepository.getObject().saveLibrary(library)
					.eventually(LoopedInPromise.response({ displayLibrary(it) }, this))
			})
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		return ViewUtils.buildStandardMenu(this, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return drawerToggle.isCreated
			&& drawerToggle.getObject().onOptionsItemSelected(item)
			|| ViewUtils.handleMenuClicks(this, item)
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		// Sync the toggle state after onRestoreInstanceState has occurred.
		if (drawerToggle.isCreated) drawerToggle.getObject().syncState()
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		if (drawerToggle.isCreated) drawerToggle.getObject().onConfigurationChanged(newConfig)
	}

	private fun showProgressBar() {
		showContainerView(loadingViewsProgressBar.findView())
	}

	private fun showContainerView(view: View) {
		hideAllViews()
		view.visibility = View.VISIBLE
	}

	private fun hideAllViews() {
		for (i in 0 until browseLibraryContainerRelativeLayout.findView().childCount)
			browseLibraryContainerRelativeLayout.findView().getChildAt(i).visibility = View.INVISIBLE
	}

	@Synchronized
	private fun swapFragments(newFragment: Fragment) {
		val ft = supportFragmentManager.beginTransaction()
		try {
			if (activeFragment != null) ft.remove(activeFragment!!)
			ft.add(R.id.browseLibraryContainer, newFragment)
		} finally {
			ft.commit()
			activeFragment = newFragment
		}
	}

	public override fun onStop() {
		isStopped = true
		super.onStop()
	}

	override fun onBackPressed() {
		if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return
		super.onBackPressed()
	}

	override fun updateViewAnimator(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
	}

	override fun getNowPlayingFloatingActionButton(): NowPlayingFloatingActionButton {
		return nowPlayingFloatingActionButton!!
	}

	companion object {
		@JvmField
		val showDownloadsAction = MagicPropertyBuilder.buildMagicPropertyName(BrowserEntryActivity::class.java, "showDownloadsAction")!!
		private val specialViews = listOf("Active Downloads")
	}
}