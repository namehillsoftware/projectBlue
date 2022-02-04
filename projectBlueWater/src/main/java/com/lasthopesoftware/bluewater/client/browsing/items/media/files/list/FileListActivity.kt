package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import android.content.Context
import android.content.Intent
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
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.IItemListViewContainer
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.FileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemNowPlayingRegistrar
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFileProvider.Companion.fromActiveLibrary
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton.Companion.addNowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils.buildStandardMenu
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise

class FileListActivity :
	AppCompatActivity(),
	IItemListViewContainer,
	Runnable {

	companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(FileListActivity::class.java) }
		private val key by lazy { magicPropertyBuilder.buildProperty("key") }
		private val value by lazy { magicPropertyBuilder.buildProperty("value") }

		@JvmStatic
		fun startFileListActivity(context: Context, item: IItem) {
			val fileListIntent = Intent(context, FileListActivity::class.java)
			fileListIntent.putExtra(key, item.key)
			fileListIntent.putExtra(value, item.value)
			context.startActivity(fileListIntent)
		}
	}

	private val fileProvider by lazy {
		val stringListProvider = FileStringListProvider(SelectedConnectionProvider(this))
		FileProvider(stringListProvider)
	}
	private val fileListItemNowPlayingRegistrar = lazy { FileListItemNowPlayingRegistrar(MessageBus(LocalBroadcastManager.getInstance(this))) }
	private val pbLoading = LazyViewFinder<ProgressBar>(this, R.id.recyclerLoadingProgress)
	private val fileListView = LazyViewFinder<RecyclerView>(this, R.id.loadedRecyclerView)

	private lateinit var nowPlayingFloatingActionButton: NowPlayingFloatingActionButton

	private var itemId = 0
	private var viewAnimator: ViewAnimator? = null

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.layout_list_view)
		setSupportActionBar(findViewById(R.id.listViewToolbar))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		fileListView.findView().visibility = View.INVISIBLE
		pbLoading.findView().visibility = View.VISIBLE

		if (savedInstanceState != null) itemId = savedInstanceState.getInt(key)
		if (itemId == 0) itemId = this.intent.getIntExtra(key, 1)

		title = intent.getStringExtra(value)
		nowPlayingFloatingActionButton = addNowPlayingFloatingActionButton(findViewById(R.id.asynchronousRecyclerViewContainer))

		run()
	}

	override fun run() {
		val parameters = FileListParameters.getInstance().getFileListParameters(Item(itemId))
		fileProvider.promiseFiles(FileListParameters.Options.None, *parameters)
			.eventually { serviceFiles ->
				fromActiveLibrary(this)
					.eventually(LoopedInPromise.response({ l ->
						l?.let { nowPlayingFileProvider ->
							val fileListItemMenuBuilder = FileListItemMenuBuilder(
								serviceFiles,
								nowPlayingFileProvider,
								fileListItemNowPlayingRegistrar.value
							)

							ItemListMenuChangeHandler(this).apply {
								fileListItemMenuBuilder.setOnViewChangedListener(
									ViewChangedHandler()
										.setOnViewChangedListener(this)
										.setOnAnyMenuShown(this)
										.setOnAllMenusHidden(this)
								)
							}

							val fileListView = fileListView.findView()
							fileListView.adapter = FileListAdapter(serviceFiles, fileListItemMenuBuilder)
							val layoutManager = LinearLayoutManager(this)
							fileListView.layoutManager = layoutManager
							fileListView.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
							fileListView.visibility = View.VISIBLE

							pbLoading.findView().visibility = View.INVISIBLE
						}
					}, this))
			}
			.excuse(HandleViewIoException(this, this))
			.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(this), this))
			.then { finish() }
	}

	override fun onStart() {
		super.onStart()
		restoreSelectedConnection(this)
	}

	override fun onSaveInstanceState(savedInstanceState: Bundle) {
		super.onSaveInstanceState(savedInstanceState)
		savedInstanceState.putInt(key, itemId)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		itemId = savedInstanceState.getInt(key)
	}

	override fun onCreateOptionsMenu(menu: Menu) = buildStandardMenu(menu)

	override fun onOptionsItemSelected(item: MenuItem) =
		ViewUtils.handleNavMenuClicks(this, item) || super.onOptionsItemSelected(item)

	override fun onBackPressed() {
		if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return
		super.onBackPressed()
	}

	override fun updateViewAnimator(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
	}

	override fun getNowPlayingFloatingActionButton() = nowPlayingFloatingActionButton

	override fun onDestroy() {
		super.onDestroy()

		if (fileListItemNowPlayingRegistrar.isInitialized()) fileListItemNowPlayingRegistrar.value.clear()
	}
}
