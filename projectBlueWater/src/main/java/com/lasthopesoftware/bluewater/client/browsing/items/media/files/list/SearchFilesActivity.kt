package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ProgressBar
import android.widget.ViewAnimator
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.list.IItemListViewContainer
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.FileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.SearchFileParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemNowPlayingRegistrar
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity.Companion.restoreSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.promiseSelectedConnection
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFileProvider.Companion.fromActiveLibrary
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton.Companion.addNowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise

class SearchFilesActivity : AppCompatActivity(), IItemListViewContainer {

	private class ScopedConnectionProviderDependencies(val connectionProvider: IConnectionProvider) {
		val lazyFileProvider = lazy {
			val stringListProvider = FileStringListProvider(connectionProvider)
			FileProvider(stringListProvider)
		}
	}

	private val pbLoading = LazyViewFinder<ProgressBar>(this, R.id.recyclerLoadingProgress)
	private val fileListView = LazyViewFinder<RecyclerView>(this, R.id.loadedRecyclerView)
	private val lazyScopedConnectionProviderDependencies = lazy {
		promiseSelectedConnection().then { ScopedConnectionProviderDependencies(it!!) }
	}
	private var viewAnimator: ViewAnimator? = null
	private var nowPlayingFloatingActionButton: NowPlayingFloatingActionButton? = null

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		setContentView(R.layout.asynchronous_recycler_view)

		fileListView.findView().visibility = View.INVISIBLE
		pbLoading.findView().visibility = View.VISIBLE
		nowPlayingFloatingActionButton = addNowPlayingFloatingActionButton(findViewById(R.id.asynchronousRecyclerViewContainer))

		handleIntent(intent)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		return ViewUtils.buildStandardMenu(this, menu)
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		handleIntent(intent)
	}

	private fun handleIntent(intent: Intent) {
		if (Intent.ACTION_SEARCH != intent.action) return

		val query = intent.getStringExtra(SearchManager.QUERY)
		if (query.isNullOrEmpty()) return
		title = getString(R.string.title_activity_search_results).format(query)

		fileListView.findView().visibility = View.VISIBLE
		pbLoading.findView().visibility = View.INVISIBLE

		val context = this
		(object : Runnable {
			override fun run() {
				lazyScopedConnectionProviderDependencies.value
					.eventually { dependencies ->
						val parameters = SearchFileParameterProvider.getFileListParameters(query)
						dependencies.lazyFileProvider.value.promiseFiles(FileListParameters.Options.None, *parameters)
							.eventually(LoopedInPromise.response({ serviceFiles ->
								fromActiveLibrary(context)
									?.let { nowPlayingFileProvider ->
										FileListItemMenuBuilder(
											serviceFiles,
											nowPlayingFileProvider,
											dependencies.connectionProvider,
											FileListItemNowPlayingRegistrar(LocalBroadcastManager.getInstance(context)))
									}
									?.also { fileListItemMenuBuilder ->
										ItemListMenuChangeHandler(context).apply {
											fileListItemMenuBuilder.setOnViewChangedListener(
												ViewChangedHandler()
													.setOnViewChangedListener(this)
													.setOnAnyMenuShown(this)
													.setOnAllMenusHidden(this))
										}

										val fileListView = fileListView.findView()
										fileListView.adapter = FileListAdapter(serviceFiles, fileListItemMenuBuilder)
										val layoutManager = LinearLayoutManager(context)
										fileListView.layoutManager = layoutManager
										fileListView.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))
										fileListView.visibility = View.VISIBLE
										pbLoading.findView().visibility = View.INVISIBLE
									}
							}, context))
					}
					.excuse(HandleViewIoException(context, this))
					.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(context), context))
					.then { finish() }
			}
		}).run()
	}

	public override fun onStart() {
		super.onStart()
		restoreSelectedConnection(this)
	}

	override fun onBackPressed() {
		if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return
		super.onBackPressed()
	}

	override fun updateViewAnimator(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
	}

	override fun getNowPlayingFloatingActionButton(): NowPlayingFloatingActionButton? {
		return nowPlayingFloatingActionButton
	}
}
