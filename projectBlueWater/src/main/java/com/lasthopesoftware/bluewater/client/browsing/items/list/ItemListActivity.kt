package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.ViewAnimator
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.MenuNotifications
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton.Companion.addNowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class ItemListActivity : AppCompatActivity(), IItemListViewContainer, ImmediateResponse<List<Item>?, Unit> {
	private var connectionRestoreCode: Int? = null
	private val itemProviderComplete by lazy { LoopedInPromise.response(this, this) }
	private val itemListView by lazy {
		val recyclerView = findViewById<RecyclerView>(R.id.loadedRecyclerView)
		promisedItemListAdapter.eventually(LoopedInPromise.response({ adapter ->
			recyclerView.adapter = adapter
			val layoutManager = LinearLayoutManager(this)
			recyclerView.layoutManager = layoutManager
			recyclerView.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
		}, this))
		recyclerView
	}
	private val pbLoading = LazyViewFinder<ProgressBar>(this, R.id.recyclerLoadingProgress)
	private val specificLibraryProvider by lazy {
			SelectedBrowserLibraryProvider(
				SelectedBrowserLibraryIdentifierProvider(getApplicationSettingsRepository()),
				LibraryRepository(this))
		}
	private val lazyFileStringListProvider by lazy { FileStringListProvider(SelectedConnectionProvider(this)) }

	private val promisedItemProvider by lazy {
		getInstance(this).promiseSessionConnection().then { c -> c?.let(::ItemProvider) }
	}

	private val messageBus by lazy { MessageBus(LocalBroadcastManager.getInstance(this)) }

	private val promisedItemListAdapter: Promise<ItemListAdapter?> by lazy {
		specificLibraryProvider.browserLibrary
			.eventually {
				it?.let { l ->
					promisedItemProvider.then { itemProvider ->
						itemProvider?.let {
							val storedItemAccess = StoredItemAccess(this)

							ItemListAdapter(
								this,
								messageBus,
								FileListParameters.getInstance(),
								lazyFileStringListProvider,
								ItemListMenuChangeHandler(this),
								storedItemAccess,
								itemProvider,
								l
							)
						}
					}
				}.keepPromise()
			}
	}

	private val launchingActivityReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			val isLaunching = intent?.action == MenuNotifications.launchingActivity

			itemListView.visibility = ViewUtils.getVisibility(!isLaunching)
			pbLoading.findView().visibility = ViewUtils.getVisibility(isLaunching)
		}
	}

	private lateinit var nowPlayingFloatingActionButton: NowPlayingFloatingActionButton
	private var viewAnimator: ViewAnimator? = null
	private var mItemId = 0

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.layout_list_view)
		setSupportActionBar(findViewById(R.id.listViewToolbar))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		val intentFilter = IntentFilter()
		intentFilter.addAction(MenuNotifications.launchingActivity)
		intentFilter.addAction(MenuNotifications.launchingActivityFinished)
		messageBus.registerReceiver(
			launchingActivityReceiver,
			intentFilter
		)

		mItemId = 0
		if (savedInstanceState != null) mItemId = savedInstanceState.getInt(KEY)
		if (mItemId == 0) mItemId = intent.getIntExtra(KEY, 0)
		title = intent.getStringExtra(VALUE)
		nowPlayingFloatingActionButton = addNowPlayingFloatingActionButton(findViewById(R.id.asynchronousRecyclerViewContainer))
	}

	public override fun onStart() {
		super.onStart()
		restoreSelectedConnection(this).eventually(LoopedInPromise.response({
			connectionRestoreCode = it
			if (it == null) hydrateItems()
		}, this))
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == connectionRestoreCode) hydrateItems()
		super.onActivityResult(requestCode, resultCode, data)
	}

	private fun hydrateItems() {
		itemListView.visibility = View.INVISIBLE
		pbLoading.findView().visibility = View.VISIBLE
		promisedItemProvider
			.eventually { p -> p?.promiseItems(mItemId).keepPromise(emptyList()) }
			.eventually(itemProviderComplete)
			.excuse(HandleViewIoException(this, ::hydrateItems))
			.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(this), this))
			.then { finish() }
	}

	override fun respond(items: List<Item>?) {
		items ?: return

		promisedItemListAdapter
			.eventually { adapter ->
				adapter?.updateListEventually(items).keepPromise(Unit)
			}
			.eventually(LoopedInPromise.response({
				itemListView.visibility = ViewUtils.getVisibility(true)
				pbLoading.findView().visibility = ViewUtils.getVisibility(false)
			}, this))
	}

	public override fun onSaveInstanceState(savedInstanceState: Bundle) {
		super.onSaveInstanceState(savedInstanceState)
		savedInstanceState.putInt(KEY, mItemId)
	}

	public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		mItemId = savedInstanceState.getInt(KEY)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean = ViewUtils.buildStandardMenu(this, menu)

	override fun onOptionsItemSelected(item: MenuItem): Boolean =
		ViewUtils.handleNavMenuClicks(this, item) || super.onOptionsItemSelected(item)

	override fun onBackPressed() {
		if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return
		super.onBackPressed()
	}

	override fun updateViewAnimator(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
	}

	override fun getNowPlayingFloatingActionButton(): NowPlayingFloatingActionButton = nowPlayingFloatingActionButton

	override fun onDestroy() {
		messageBus.unregisterReceiver(launchingActivityReceiver)
		super.onDestroy()
	}

	companion object {
		private val magicPropertyBuilder = MagicPropertyBuilder(ItemListActivity::class.java)
		@JvmField
		val KEY = magicPropertyBuilder.buildProperty("key")
		@JvmField
		val VALUE = magicPropertyBuilder.buildProperty("value")
	}
}
