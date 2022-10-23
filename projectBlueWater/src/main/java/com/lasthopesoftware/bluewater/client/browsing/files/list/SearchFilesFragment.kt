package com.lasthopesoftware.bluewater.client.browsing.files.list

import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.menu.FileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.SelectedLibraryUrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingFileProvider.Companion.fromActiveLibrary
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.databinding.AsynchronousSearchViewBinding
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SearchFilesFragment : Fragment(), View.OnKeyListener, TextView.OnEditorActionListener {

	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null

	private val selectedLibraryIdProvider by lazy { requireContext().getCachedSelectedLibraryIdProvider() }

	private val libraryConnectionProvider by lazy { ConnectionSessionManager.get(requireContext()) }

	private val fileProvider by lazy {
		val stringListProvider = LibraryFileStringListProvider(libraryConnectionProvider)
		LibraryFileProvider(stringListProvider)
	}

	private val scopedUrlKeyProvider by lazy {
		SelectedLibraryUrlKeyProvider(
			selectedLibraryIdProvider,
			UrlKeyProvider(libraryConnectionProvider)
		)
	}

	private val scopedMessageBus = lazy {
		getApplicationMessageBus().getScopedMessageBus()
	}

	private val nowPlayingFileProvider by lazy { fromActiveLibrary(requireContext()) }

	private val searchFilesViewModel by buildViewModelLazily {
		SearchFilesViewModel(
			selectedLibraryIdProvider,
			fileProvider,
			PlaybackServiceController(requireContext()),
		)
	}

	private val handler by lazy { Handler(requireContext().mainLooper) }

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val binding = DataBindingUtil.inflate<AsynchronousSearchViewBinding>(
			inflater,
			R.layout.asynchronous_search_view,
			container,
			false
		)

		binding.vm = searchFilesViewModel

		searchFilesViewModel.files.onEach { serviceFiles ->
			nowPlayingFileProvider
				.eventually(LoopedInPromise.response({
					it
						?.let { nowPlayingFileProvider ->
							scopedMessageBus.value.close()
							FileListItemMenuBuilder(
								serviceFiles,
								nowPlayingFileProvider,
								scopedMessageBus.value,
								scopedUrlKeyProvider,
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

							binding.resultsContainer.loadedRecyclerView.apply {
								adapter = FileListAdapter(serviceFiles, fileListItemMenuBuilder)
								val newLayoutManager = LinearLayoutManager(context)
								layoutManager = newLayoutManager
								addItemDecoration(DividerItemDecoration(context, newLayoutManager.orientation))
							}
						}
				}, handler))
		}.launchIn(lifecycleScope)

		return binding.run {
			searchPrompt.apply {
				this@SearchFilesFragment
					.also(::setOnEditorActionListener)
					.also(::setOnKeyListener)
				imeOptions = EditorInfo.IME_ACTION_SEARCH
				setImeActionLabel(context.getString(R.string.lbl_search), KeyEvent.KEYCODE_ENTER)
			}

			searchLayout
		}
	}

	override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
		if (keyCode != KeyEvent.KEYCODE_ENTER || event?.action != KeyEvent.ACTION_UP) return false

		searchFilesViewModel.findFiles()
		v?.let(ViewCompat::getWindowInsetsController)?.hide(WindowInsetsCompat.Type.ime())

		return true
	}

	override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
		if (actionId != EditorInfo.IME_ACTION_SEARCH) return false

		v?.apply {
			searchFilesViewModel.findFiles()
			ViewCompat.getWindowInsetsController(this)?.hide(WindowInsetsCompat.Type.ime())
		}

		return true
	}

	override fun onDestroy() {
		super.onDestroy()

		if (scopedMessageBus.isInitialized()) scopedMessageBus.value.close()
	}

	fun setOnItemListMenuChangeHandler(itemListMenuChangeHandler: IItemListMenuChangeHandler?) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler
	}
}
