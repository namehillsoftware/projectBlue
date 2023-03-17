package com.lasthopesoftware.bluewater.client.servers.list

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectBrowserLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.servers.list.listeners.EditServerClickListener
import com.lasthopesoftware.bluewater.client.servers.list.listeners.SelectServerOnClickListener
import com.lasthopesoftware.bluewater.shared.android.adapters.DeferredListAdapter
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.android.view.getValue
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.namehillsoftware.handoff.promises.Promise

class ServerListAdapter(private val activity: Activity, private val browserLibrarySelection: SelectBrowserLibrary, private val registerForApplicationMessages: RegisterForApplicationMessages)
	: DeferredListAdapter<Library, ServerListAdapter.ViewHolder>(activity, LibraryDiffer) {

	private val handler by lazy { Handler(activity.mainLooper) }
	private var activeLibrary: Library? = null

	fun updateLibraries(libraries: Collection<Library>, activeLibrary: Library?): Promise<Unit> {
		this.activeLibrary = activeLibrary
		return updateListEventually(libraries.toList())
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val relativeLayout = getInflater(parent.context).inflate(R.layout.layout_server_item, parent, false) as RelativeLayout
		return ViewHolder(relativeLayout)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.update(getItem(position))
	}

	companion object {
		private fun getInflater(context: Context): LayoutInflater {
			return context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		}
	}

	inner class ViewHolder(private val parent: RelativeLayout) : RecyclerView.ViewHolder(parent) {
		private val textView by LazyViewFinder<TextView>(parent, R.id.tvServerItem)
		private val btnSelectServer by LazyViewFinder<Button>(parent, R.id.btnSelectServer)
		private val btnConfigureServer by LazyViewFinder<ImageButton>(parent, R.id.btnConfigureServer)

		private var librarySelectionChangedSubscription: AutoCloseable? = null
		private var onAttachStateChangeListener: View.OnAttachStateChangeListener? = null

		fun update(library: Library) {
			textView.text = library.accessCode
			textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(library.id == activeLibrary?.id))

			librarySelectionChangedSubscription?.close()
			librarySelectionChangedSubscription = registerForApplicationMessages
				.registerReceiver(handler) { m: BrowserLibrarySelection.LibraryChosenMessage ->
					textView.setTypeface(
						null,
						ViewUtils.getActiveListItemTextViewStyle(library.id == m.chosenLibraryId.id)
					)
				}

			onAttachStateChangeListener?.also(parent::removeOnAttachStateChangeListener)
			parent.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
				override fun onViewAttachedToWindow(v: View) {}
				override fun onViewDetachedFromWindow(v: View) {
					librarySelectionChangedSubscription?.close()
				}
			}.apply { onAttachStateChangeListener = this })

			btnSelectServer.setOnClickListener(SelectServerOnClickListener(library, browserLibrarySelection))
			btnConfigureServer.setOnClickListener(EditServerClickListener(activity, library.id))
		}
	}

	private object LibraryDiffer : DiffUtil.ItemCallback<Library>() {
		override fun areItemsTheSame(oldItem: Library, newItem: Library): Boolean = oldItem.id == newItem.id

		override fun areContentsTheSame(oldItem: Library, newItem: Library): Boolean = areItemsTheSame(oldItem, newItem)
	}
}
