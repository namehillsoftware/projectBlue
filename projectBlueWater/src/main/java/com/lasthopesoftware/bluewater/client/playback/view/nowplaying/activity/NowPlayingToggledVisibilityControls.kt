package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder

internal class NowPlayingToggledVisibilityControls(
	private val nowPlayingListHandle: LazyViewFinder<ImageView>,
    private val playerControlsLinearLayout: LazyViewFinder<LinearLayout>,
    private val menuControlsLinearLayout: LazyViewFinder<LinearLayout>,
    private val ratingBarLinearLayout: LazyViewFinder<LinearLayout>
) {
    var isVisible = true
        private set

    fun toggleVisibility(isVisible: Boolean) {
        this.isVisible = isVisible
        val normalVisibility = if (isVisible) View.VISIBLE else View.INVISIBLE

		nowPlayingListHandle.findView().visibility = normalVisibility
        playerControlsLinearLayout.findView().visibility = normalVisibility
		ratingBarLinearLayout.findView().visibility = normalVisibility

		// Make this view gone so that song title text can take up full view when not displayed
        menuControlsLinearLayout.findView().visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}
