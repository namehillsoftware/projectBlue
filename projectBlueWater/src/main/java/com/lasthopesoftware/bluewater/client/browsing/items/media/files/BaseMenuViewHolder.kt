package com.lasthopesoftware.bluewater.client.browsing.items.media.files

import android.widget.ImageButton
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder

open class
BaseMenuViewHolder(
	private val viewFileDetailsButtonFinder: LazyViewFinder<ImageButton>,
	private val playButtonFinder: LazyViewFinder<ImageButton>)
{
	val viewFileDetailsButton: ImageButton
		get() = viewFileDetailsButtonFinder.findView()

	val playButton: ImageButton
		get() = playButtonFinder.findView()
}
