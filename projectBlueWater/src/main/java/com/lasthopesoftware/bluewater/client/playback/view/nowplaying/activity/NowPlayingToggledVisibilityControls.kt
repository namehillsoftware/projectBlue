package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity

import android.view.View
import android.widget.LinearLayout
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder

/**
 * Created by david on 10/16/15.
 */
internal class NowPlayingToggledVisibilityControls(
    private val playerControlsLinearLayout: LazyViewFinder<LinearLayout>,
    private val menuControlsLinearLayout: LazyViewFinder<LinearLayout>,
    private val ratingBarLinearLayout: LazyViewFinder<LinearLayout>
) {
    var isVisible = true
        private set

    fun toggleVisibility(isVisible: Boolean) {
        this.isVisible = isVisible
        val normalVisibility = if (isVisible) View.VISIBLE else View.INVISIBLE

        playerControlsLinearLayout.findView().visibility = normalVisibility
		ratingBarLinearLayout.findView().visibility = normalVisibility

		// Make this view gone so that song title text can take up full view when not displayed
        menuControlsLinearLayout.findView().visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}
