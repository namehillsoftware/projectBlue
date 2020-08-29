package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ProgressBar
import android.widget.ViewAnimator
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.list.IItemListViewContainer
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.FileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.SearchFileParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity.Companion.restoreSessionConnection
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFileProvider.Companion.fromActiveLibrary
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton.Companion.addNowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class SearchFilesActivity : AppCompatActivity(), IItemListViewContainer, ImmediateResponse<List<ServiceFile>?, Unit> {
	private val pbLoading = LazyViewFinder<ProgressBar>(this, R.id.recyclerLoadingProgress)
	private val fileListView = LazyViewFinder<RecyclerView>(this, R.id.loadedRecyclerView)
	private var viewAnimator: ViewAnimator? = null
	private var nowPlayingFloatingActionButton: NowPlayingFloatingActionButton? = null

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		setContentView(R.layout.asynchronous_recycler_view)
		fileListView.findView().visibility = View.INVISIBLE
		pbLoading.findView().visibility = View.VISIBLE
		nowPlayingFloatingActionButton = addNowPlayingFloatingActionButton(findViewById(R.id.rlViewItems))
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
		if (query == null || query.isEmpty()) return
		title = String.format(getString(R.string.title_activity_search_results), query)

		fileListView.findView().visibility = View.VISIBLE
		pbLoading.findView().visibility = View.INVISIBLE

		val onSearchFilesComplete = LoopedInPromise.response(this, this)
		(object : Runnable {
			override fun run() {
				getInstance(this@SearchFilesActivity).promiseSessionConnection()
					.eventually { connection ->
						val parameters = SearchFileParameterProvider.getFileListParameters(query)
						val stringListProvider = FileStringListProvider(connection)
						val fileProvider = FileProvider(stringListProvider)
						fileProvider.promiseFiles(FileListParameters.Options.None, *parameters)
					}
					.eventually(onSearchFilesComplete)
					.excuse(HandleViewIoException(this@SearchFilesActivity, this))
					.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(this@SearchFilesActivity), this@SearchFilesActivity))
					.then { finish() }
			}
		}).also {
			it.run()
		}
	}

	override fun respond(serviceFiles: List<ServiceFile>?) {
		if (serviceFiles == null) return

		val nowPlayingFileProvider = fromActiveLibrary(this) ?: return

		val fileListAdapter = FileListAdapter(
			serviceFiles,
			ItemListMenuChangeHandler(this),
			nowPlayingFileProvider)

		val fileListView = fileListView.findView()
		fileListView.adapter = fileListAdapter
		fileListView.layoutManager = LinearLayoutManager(this)
		fileListView.visibility = View.VISIBLE
		pbLoading.findView().visibility = View.INVISIBLE
	}

	public override fun onStart() {
		super.onStart()
		restoreSessionConnection(this)
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
