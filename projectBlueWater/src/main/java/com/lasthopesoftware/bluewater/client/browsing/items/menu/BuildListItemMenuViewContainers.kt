package com.lasthopesoftware.bluewater.client.browsing.items.menu

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

interface BuildListItemMenuViewContainers<TViewContainer : RecyclerView.ViewHolder> {
	fun buildView(parent: ViewGroup): TViewContainer
}
