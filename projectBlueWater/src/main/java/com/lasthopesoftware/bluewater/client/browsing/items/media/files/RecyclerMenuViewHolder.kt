package com.lasthopesoftware.bluewater.client.browsing.items.media.files

import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder

open class
RecyclerMenuViewHolder(
	containingView: View,
	private val viewFileDetailsButtonFinder: LazyViewFinder<ImageButton>,
	private val playButtonFinder: LazyViewFinder<ImageButton>)
	: RecyclerView.ViewHolder(containingView)
{
	val viewFileDetailsButton: ImageButton
		get() = viewFileDetailsButtonFinder.findView()

	val playButton: ImageButton
		get() = playButtonFinder.findView()
}
