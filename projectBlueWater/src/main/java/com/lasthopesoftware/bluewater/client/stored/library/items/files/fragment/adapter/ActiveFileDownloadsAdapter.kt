package com.lasthopesoftware.bluewater.client.stored.library.items.files.fragment.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.stored.library.items.files.fragment.adapter.viewholder.ActiveFileDownloadsViewHolder
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileDiffer
import com.lasthopesoftware.bluewater.shared.android.adapters.DeferredListAdapter

class ActiveFileDownloadsAdapter(context: Context) : DeferredListAdapter<StoredFile, ActiveFileDownloadsViewHolder>(
	context, StoredFileDiffer) {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveFileDownloadsViewHolder {
		val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val layout = inflater.inflate(R.layout.layout_standard_text, parent, false)
		return ActiveFileDownloadsViewHolder(layout, layout.findViewById(R.id.tvStandard))
	}

	override fun onBindViewHolder(holder: ActiveFileDownloadsViewHolder, position: Int) {
		holder.update(getItem(position))
	}
}
