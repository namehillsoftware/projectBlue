package com.lasthopesoftware.bluewater.client.stored.library.items.files.fragment.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileNameTextViewSetter
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise

class ActiveFileDownloadsViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
	var filePropertiesProvider: Promise<*> = Promise.empty<Any?>()
	private val fileNameTextViewSetter = FileNameTextViewSetter(layout.findViewById(R.id.tvStandard))

	fun update(storedFile: StoredFile) {
		filePropertiesProvider.cancel()
		filePropertiesProvider = fileNameTextViewSetter.promiseTextViewUpdate(ServiceFile(storedFile.serviceId))
	}
}
