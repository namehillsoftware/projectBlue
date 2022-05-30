package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.ViewAnimator
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.ActivityLaunching
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingFloatingActionButton.Companion.addNowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils.buildStandardMenu
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class ItemListActivity : AppCompatActivity(), IItemListViewContainer {
	private var connectionRestoreCode: Int? = null
	private val handler by lazy { Handler(mainLooper) }
	private val itemListView by lazy {
		val recyclerView = findViewById<RecyclerView>(R.id.loadedRecyclerView)
		promisedItemListAdapter.eventually(response({ adapter ->
			recyclerView.adapter = adapter
			val layoutManager = LinearLayoutManager(this)
			recyclerView.layoutManager = layoutManager
			recyclerView.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
		}, handler))
		recyclerView
	}
	private val pbLoading = LazyViewFinder<ProgressBar>(this, R.id.recyclerLoadingProgress)

	private val browserLibraryIdProvider by lazy { SelectedBrowserLibraryIdentifierProvider(getApplicationSettingsRepository()) }

	private val itemListProvider by lazy {
		val connectionProvider = ConnectionSessionManager.get(this)

		ItemStringListProvider(
            FileListParameters,
            LibraryFileStringListProvider(connectionProvider)
        )
	}

	private val itemProvider by lazy { CachedItemProvider.getInstance(this) }

	private val messageBus by lazy { getApplicationMessageBus().getScopedMessageBus() }

	private val promisedItemListAdapter: Promise<ItemListAdapter?> by lazy {
		browserLibraryIdProvider.selectedLibraryId
			.then {
				it?.let { l ->
					val storedItemAccess = StoredItemAccess(this)

					ItemListAdapter(
						this,
						messageBus,
						itemListProvider,
						ItemListMenuChangeHandler(this),
						storedItemAccess,
						itemProvider,
						l
					)
				}
			}
	}

	private lateinit var nowPlayingFloatingActionButton: NowPlayingFloatingActionButton
	private var viewAnimator: ViewAnimator? = null
	private var itemId = 0

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.layout_list_view)
		setSupportActionBar(findViewById(R.id.listViewToolbar))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		messageBus.registerReceiver { it : ActivityLaunching ->
			val isLaunching = it != ActivityLaunching.HALTED // Only show the item list view again when launching error'ed for some reason
			itemListView.visibility = ViewUtils.getVisibility(!isLaunching)
			pbLoading.findView().visibility = ViewUtils.getVisibility(isLaunching)
		}

		itemId = savedInstanceState?.getInt(KEY) ?: intent.getIntExtra(KEY, 0)
		title = intent.getStringExtra(VALUE)
		nowPlayingFloatingActionButton = addNowPlayingFloatingActionButton(findViewById(R.id.asynchronousRecyclerViewContainer))
	}

	public override fun onStart() {
		super.onStart()
		restoreSelectedConnection(this).eventually(response({ hydrateItems() }, handler))
	}

	private fun hydrateItems() {
		itemListView.visibility = ViewUtils.getVisibility(false)
		pbLoading.findView().visibility = ViewUtils.getVisibility(true)


		browserLibraryIdProvider.selectedLibraryId
			.eventually { l ->
				l?.let { itemProvider.promiseItems(l, ItemId(itemId)) }.keepPromise(emptyList())
			}
			.eventually { items ->
				promisedItemListAdapter.eventually { adapter ->
					adapter?.updateListEventually(items).keepPromise(Unit)
				}
			}
			.eventually(response({
				itemListView.visibility = ViewUtils.getVisibility(true)
				pbLoading.findView().visibility = ViewUtils.getVisibility(false)
			}, handler))
			.excuse(HandleViewIoException(this, ::hydrateItems))
			.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(this), handler))
			.then { finish() }
	}

	public override fun onSaveInstanceState(savedInstanceState: Bundle) {
		super.onSaveInstanceState(savedInstanceState)
		savedInstanceState.putInt(KEY, itemId)
	}

	public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		itemId = savedInstanceState.getInt(KEY)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean = buildStandardMenu(menu)

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
		messageBus.close()
		super.onDestroy()
	}

	companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(ItemListActivity::class.java) }

		val KEY by lazy { magicPropertyBuilder.buildProperty("key") }
		val VALUE by lazy { magicPropertyBuilder.buildProperty("value") }
	}
}
