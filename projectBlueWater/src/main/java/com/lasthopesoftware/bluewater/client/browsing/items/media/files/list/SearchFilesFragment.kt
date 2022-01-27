package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.FileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.SearchFileParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemNowPlayingRegistrar
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFileProvider.Companion.fromActiveLibrary
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

class SearchFilesFragment : Fragment(), TextView.OnEditorActionListener {

	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null

	private val fileProvider by lazy {
		val stringListProvider = FileStringListProvider(SelectedConnectionProvider(requireContext()))
		FileProvider(stringListProvider)
	}

	private val nowPlayingRegistrar by lazy {
		FileListItemNowPlayingRegistrar(LocalBroadcastManager.getInstance(requireContext()))
	}

	private var currentCancellationProxy: CancellationProxy? = null
	private var recyclerView: RecyclerView? = null
	private var progressBar: ProgressBar? = null
	private var searchPrompt: EditText? = null

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return (inflater.inflate(R.layout.asynchronous_search_view, container, false) as RelativeLayout).apply {
			progressBar = findViewById(R.id.recyclerLoadingProgress)
			recyclerView = findViewById(R.id.loadedRecyclerView)
			searchPrompt = findViewById<EditText?>(R.id.searchPrompt)?.apply {
				setOnEditorActionListener(this@SearchFilesFragment)
				imeOptions = EditorInfo.IME_ACTION_SEARCH
				setImeActionLabel(context.getString(R.string.lbl_search), KeyEvent.KEYCODE_ENTER)
			}
		}
	}

	override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
		if (actionId != EditorInfo.IME_ACTION_SEARCH) return false

		v?.text.toString().also(::doSearch)
		return true
	}

	private fun doSearch(query: String) {
		val context = context ?: return

		currentCancellationProxy?.run()
		val newCancellationProxy = CancellationProxy()
		currentCancellationProxy = newCancellationProxy

		recyclerView?.visibility = View.VISIBLE
		progressBar?.visibility = View.INVISIBLE

		(object : Runnable {
			override fun run() {
				if (newCancellationProxy.isCancelled) return

				val parameters = SearchFileParameterProvider.getFileListParameters(query)
				fileProvider
					.promiseFiles(FileListParameters.Options.None, *parameters)
					.also(newCancellationProxy::doCancel)
					.eventually { serviceFiles ->
						if (newCancellationProxy.isCancelled) Unit.toPromise()
						else fromActiveLibrary(context)
							.eventually(LoopedInPromise.response({
								if (newCancellationProxy.isCancelled) Unit
								else it
									?.let { nowPlayingFileProvider ->
										FileListItemMenuBuilder(
											serviceFiles,
											nowPlayingFileProvider,
											nowPlayingRegistrar
										)
									}
									?.also { fileListItemMenuBuilder ->
										itemListMenuChangeHandler?.apply {
											fileListItemMenuBuilder.setOnViewChangedListener(
												ViewChangedHandler()
													.setOnViewChangedListener(this)
													.setOnAnyMenuShown(this)
													.setOnAllMenusHidden(this)
											)
										}

										recyclerView?.apply {
											adapter = FileListAdapter(serviceFiles, fileListItemMenuBuilder)
											val newLayoutManager = LinearLayoutManager(context)
											layoutManager = newLayoutManager
											addItemDecoration(DividerItemDecoration(context, newLayoutManager.orientation))
											visibility = View.VISIBLE
										}
										progressBar?.visibility = View.INVISIBLE
									}
							}, context))
					}
					.excuse(HandleViewIoException(context, this))
					.eventuallyExcuse(LoopedInPromise.response(UnexpectedExceptionToasterResponse(context), context))
			}
		}).run()
	}

	fun setOnItemListMenuChangeHandler(itemListMenuChangeHandler: IItemListMenuChangeHandler?) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler
	}
}
