package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.widget.ViewAnimator
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener.Companion.tryFlipToPreviousView
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingFloatingActionButton.Companion.addNowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.databinding.LayoutListViewBinding
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils.buildStandardMenu
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import com.lasthopesoftware.bluewater.shared.promises.extensions.toAsync
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ItemListActivity : AppCompatActivity(), IItemListViewContainer, Runnable {

	private val handler by lazy { Handler(mainLooper) }

	private val itemListProvider by lazy {
		val connectionProvider = ConnectionSessionManager.get(this)

		ItemStringListProvider(
			FileListParameters,
			LibraryFileStringListProvider(connectionProvider)
		)
	}

	private val promisedItemListAdapter by lazy {
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

	private val browserLibraryIdProvider by lazy { SelectedBrowserLibraryIdentifierProvider(getApplicationSettingsRepository()) }

	private val itemProvider by lazy { CachedItemProvider.getInstance(this) }

	private val messageBus by lazy { getApplicationMessageBus() }

	private val viewModel by buildViewModelLazily {
		ItemListViewModel(browserLibraryIdProvider, itemProvider, messageBus)
	}

	private lateinit var nowPlayingFloatingActionButton: NowPlayingFloatingActionButton
	private var viewAnimator: ViewAnimator? = null
	private var itemId = 0

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val binding = DataBindingUtil.setContentView<LayoutListViewBinding>(this, R.layout.layout_list_view)
		binding.lifecycleOwner = this
		binding.vm = viewModel

		val recyclerView = with (binding) {
			setSupportActionBar(listViewToolbar)
			supportActionBar?.setDisplayHomeAsUpEnabled(true)

			itemId = savedInstanceState?.getInt(KEY) ?: intent.getIntExtra(KEY, 0)
			title = intent.getStringExtra(VALUE)
			nowPlayingFloatingActionButton = addNowPlayingFloatingActionButton(listContainer.asynchronousRecyclerViewContainer)
			listContainer.loadedRecyclerView
		}

		promisedItemListAdapter.eventually(response({ adapter ->
			recyclerView.adapter = adapter
			val layoutManager = LinearLayoutManager(this)
			recyclerView.layoutManager = layoutManager
			recyclerView.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))

			viewModel.items.onEach {
				adapter.updateListEventually(it).toAsync().await()
			}.launchIn(lifecycleScope)
		}, handler))
	}

	public override fun onStart() {
		super.onStart()
		restoreSelectedConnection(this).eventually(response({ run() }, handler))
	}

	override fun run() {
		viewModel
			.loadItems(ItemId(itemId))
			.excuse(HandleViewIoException(this, this))
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
		if (viewAnimator?.tryFlipToPreviousView() == true) return
		super.onBackPressed()
	}

	override fun updateViewAnimator(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
	}

	override fun getNowPlayingFloatingActionButton(): NowPlayingFloatingActionButton = nowPlayingFloatingActionButton

	companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(ItemListActivity::class.java) }

		val KEY by lazy { magicPropertyBuilder.buildProperty("key") }
		val VALUE by lazy { magicPropertyBuilder.buildProperty("value") }
	}
}
