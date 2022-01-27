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
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

class SearchFilesFragment : Fragment(), View.OnKeyListener, TextView.OnEditorActionListener {

	companion object {
		private val searchPromptKey by lazy { MagicPropertyBuilder.buildMagicPropertyName<SearchFilesFragment>("searchPromptKey") }
	}

	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null

	private val fileProvider by lazy {
		val stringListProvider = FileStringListProvider(SelectedConnectionProvider(requireContext()))
		FileProvider(stringListProvider)
	}

	private val nowPlayingRegistrar by lazy {
		FileListItemNowPlayingRegistrar(LocalBroadcastManager.getInstance(requireContext()))
	}

	private val nowPlayingFileProvider by lazy { fromActiveLibrary(requireContext()) }

	private var currentCancellationProxy: CancellationProxy? = null
	private var recyclerView: RecyclerView? = null
	private var progressBar: ProgressBar? = null
	private var searchPrompt: EditText? = null
	private var currentSearchPrompt: String? = null

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return (inflater.inflate(R.layout.asynchronous_search_view, container, false) as RelativeLayout).apply {
			progressBar = findViewById(R.id.recyclerLoadingProgress)
			recyclerView = findViewById(R.id.loadedRecyclerView)
			searchPrompt = findViewById<EditText?>(R.id.searchPrompt)?.apply {
				setOnEditorActionListener(this@SearchFilesFragment)
				setOnKeyListener(this@SearchFilesFragment)
				imeOptions = EditorInfo.IME_ACTION_SEARCH
				setImeActionLabel(context.getString(R.string.lbl_search), KeyEvent.KEYCODE_ENTER)
			}
		}
	}

	override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
		if (keyCode != KeyEvent.KEYCODE_ENTER || event?.action != KeyEvent.ACTION_UP) return false

		searchPrompt?.text.toString().also(::doSearch)
		return true
	}

	override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
		if (actionId != EditorInfo.IME_ACTION_SEARCH) return false

		v?.text.toString().also(::doSearch)
		return true
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)

		outState.putString(searchPromptKey, currentSearchPrompt)
	}

	override fun onViewStateRestored(savedInstanceState: Bundle?) {
		super.onViewStateRestored(savedInstanceState)

		savedInstanceState?.getString(searchPromptKey)?.also{
			searchPrompt?.setText(it)
			doSearch(it)
		}
	}

	private fun doSearch(query: String) {
		currentSearchPrompt = query
		currentCancellationProxy?.run()
		val newCancellationProxy = CancellationProxy()
		currentCancellationProxy = newCancellationProxy

		recyclerView?.visibility = View.VISIBLE
		progressBar?.visibility = View.INVISIBLE

		SearchAction(query, newCancellationProxy).run()
	}

	fun setOnItemListMenuChangeHandler(itemListMenuChangeHandler: IItemListMenuChangeHandler?) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler
	}

	private inner class SearchAction(private val query: String, private val cancellationProxy: CancellationProxy) : Runnable {
		override fun run() {
			val context = context ?: return
			if (cancellationProxy.isCancelled) return

			val parameters = SearchFileParameterProvider.getFileListParameters(query)
			fileProvider
				.promiseFiles(FileListParameters.Options.None, *parameters)
				.also(cancellationProxy::doCancel)
				.eventually { serviceFiles ->
					if (cancellationProxy.isCancelled) Unit.toPromise()
					else nowPlayingFileProvider
						.eventually(LoopedInPromise.response({
							if (cancellationProxy.isCancelled) Unit
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
	}
}
