package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.ViewAnimator
import androidx.appcompat.app.AppCompatActivity
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity.Companion.restoreSessionConnection
import com.lasthopesoftware.bluewater.client.connection.session.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton.Companion.addNowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class ItemListActivity : AppCompatActivity(), IItemListViewContainer, ImmediateResponse<List<Item>?, Unit> {
	private val itemProviderComplete = lazy { LoopedInPromise.response(this, this) }
	private val itemListView = LazyViewFinder<ListView>(this, R.id.lvItems)
	private val pbLoading = LazyViewFinder<ProgressBar>(this, R.id.pbLoadingItems)
	private val lazySpecificLibraryProvider = lazy {
			SelectedBrowserLibraryProvider(
				SelectedBrowserLibraryIdentifierProvider(this),
				LibraryRepository(this))
		}

	private lateinit var nowPlayingFloatingActionButton: NowPlayingFloatingActionButton
	private var viewAnimator: ViewAnimator? = null
	private var mItemId = 0

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_view_items)
		setSupportActionBar(findViewById(R.id.viewItemsToolbar))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		mItemId = 0
		if (savedInstanceState != null) mItemId = savedInstanceState.getInt(KEY)
		if (mItemId == 0) mItemId = intent.getIntExtra(KEY, 0)
		title = intent.getStringExtra(VALUE)
		nowPlayingFloatingActionButton = addNowPlayingFloatingActionButton(findViewById(R.id.rlViewItems))
	}

	public override fun onStart() {
		super.onStart()
		val doRestore = restoreSessionConnection(this)
		if (!doRestore) hydrateItems()
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) hydrateItems()
		super.onActivityResult(requestCode, resultCode, data)
	}

	private fun hydrateItems() {
		itemListView.findView().visibility = View.INVISIBLE
		pbLoading.findView().visibility = View.VISIBLE
		getInstance(this).promiseSessionConnection()
			.eventually { c ->
				val itemProvider = ItemProvider(c)
				itemProvider.promiseItems(mItemId)
			}
			.eventually(itemProviderComplete.value)
			.excuse(HandleViewIoException(this) { hydrateItems() })
			.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(this), this))
			.then { finish() }
	}

	override fun respond(items: List<Item>?) {
		items?.let { buildItemListView(it) }
	}

	private fun buildItemListView(items: List<Item>) {
		lazySpecificLibraryProvider.value.browserLibrary
			.eventually(LoopedInPromise.response({ library ->
				val storedItemAccess = StoredItemAccess(this)
				val itemListAdapter = ItemListAdapter(
					this,
					R.id.tvStandard,
					items,
					FileListParameters.getInstance(),
					ItemListMenuChangeHandler(this),
					storedItemAccess,
					library)
				val localItemListView = itemListView.findView()
				localItemListView.adapter = itemListAdapter
				localItemListView.onItemClickListener = ClickItemListener(items, pbLoading.findView())
				localItemListView.onItemLongClickListener = LongClickViewAnimatorListener()
				itemListView.findView().visibility = View.VISIBLE
				pbLoading.findView().visibility = View.INVISIBLE
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

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		return ViewUtils.buildStandardMenu(this, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return ViewUtils.handleNavMenuClicks(this, item) || super.onOptionsItemSelected(item)
	}

	override fun onBackPressed() {
		if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return
		super.onBackPressed()
	}

	override fun updateViewAnimator(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
	}

	override fun getNowPlayingFloatingActionButton(): NowPlayingFloatingActionButton = nowPlayingFloatingActionButton

	companion object {
		private val magicPropertyBuilder = MagicPropertyBuilder(ItemListActivity::class.java)
		@JvmField
		val KEY = magicPropertyBuilder.buildProperty("key")
		@JvmField
		val VALUE = magicPropertyBuilder.buildProperty("value")
	}
}
