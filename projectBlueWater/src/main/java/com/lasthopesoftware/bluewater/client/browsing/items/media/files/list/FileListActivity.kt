package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import android.content.Context
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
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.IItemListViewContainer
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.FileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity.Companion.restoreSessionConnection
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFileProvider.Companion.fromActiveLibrary
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton.Companion.addNowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import com.namehillsoftware.lazyj.Lazy
import java.util.concurrent.Callable

class FileListActivity : AppCompatActivity(), IItemListViewContainer, ImmediateResponse<List<ServiceFile>?, Unit>, Runnable, Callable<PromisedResponse<List<ServiceFile>?, Unit>> {

	private var itemId = 0
	private val pbLoading = LazyViewFinder<ProgressBar>(this, R.id.pbLoadingItems)
	private val fileListView = LazyViewFinder<ListView>(this, R.id.lvItems)
	private val onFileProviderComplete = Lazy(this)

	private var viewAnimator: ViewAnimator? = null
	private var nowPlayingFloatingActionButton: NowPlayingFloatingActionButton? = null

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.activity_view_items)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		fileListView.findView().visibility = View.INVISIBLE
		pbLoading.findView().visibility = View.VISIBLE

		if (savedInstanceState != null) itemId = savedInstanceState.getInt(key)
		if (itemId == 0) itemId = this.intent.getIntExtra(key, 1)

		title = intent.getStringExtra(value)
		nowPlayingFloatingActionButton = addNowPlayingFloatingActionButton(findViewById(R.id.rlViewItems))

		run()
	}

	override fun run() {
		getInstance(this).promiseSessionConnection()
			.eventually { connection ->
				val parameters = FileListParameters.getInstance().getFileListParameters(Item(itemId))
				val stringListProvider = FileStringListProvider(connection)
				val fileProvider = FileProvider(stringListProvider)
				fileProvider.promiseFiles(FileListParameters.Options.None, *parameters)
			}
			.eventually(onFileProviderComplete.getObject())
			.excuse(HandleViewIoException(this, this))
			.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(this), this))
			.then { finish() }
	}

	override fun call(): PromisedResponse<List<ServiceFile>?, Unit> =
		LoopedInPromise.response<List<ServiceFile>?, Unit>(this, this)

	override fun respond(serviceFiles: List<ServiceFile>?) {
		if (serviceFiles == null) return

		val longClickViewAnimatorListener = LongClickViewAnimatorListener()
		fileListView.findView().onItemLongClickListener = longClickViewAnimatorListener

		val fileListAdapter = FileListAdapter(
			this,
			R.id.tvStandard,
			serviceFiles,
			ItemListMenuChangeHandler(this),
			fromActiveLibrary(this))

		fileListView.findView().adapter = fileListAdapter

		fileListView.findView().visibility = View.VISIBLE
		pbLoading.findView().visibility = View.INVISIBLE
	}

	public override fun onStart() {
		super.onStart()
		restoreSessionConnection(this)
	}

	public override fun onSaveInstanceState(savedInstanceState: Bundle) {
		super.onSaveInstanceState(savedInstanceState)
		savedInstanceState.putInt(key, itemId)
	}

	public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		itemId = savedInstanceState.getInt(key)
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

	override fun getNowPlayingFloatingActionButton(): NowPlayingFloatingActionButton {
		return nowPlayingFloatingActionButton!!
	}

	companion object {
		private val magicPropertyBuilder = MagicPropertyBuilder(FileListActivity::class.java)
		private val key = magicPropertyBuilder.buildProperty("key")
		private val value = magicPropertyBuilder.buildProperty("value")
		@JvmStatic
		fun startFileListActivity(context: Context, item: IItem) {
			val fileListIntent = Intent(context, FileListActivity::class.java)
			fileListIntent.putExtra(key, item.key)
			fileListIntent.putExtra(value, item.value)
			context.startActivity(fileListIntent)
		}
	}
}
