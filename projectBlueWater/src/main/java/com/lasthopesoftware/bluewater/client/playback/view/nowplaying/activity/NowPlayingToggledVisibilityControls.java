package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.RatingBar;

import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;

/**
 * Created by david on 10/16/15.
 */
class NowPlayingToggledVisibilityControls {
    private final LazyViewFinder<LinearLayout> playerControlsLinearLayout;
    private final LazyViewFinder<LinearLayout> menuControlsLinearLayout;
    private final LazyViewFinder<RatingBar> ratingBar;

    private boolean isVisible = true;

    NowPlayingToggledVisibilityControls(LazyViewFinder<LinearLayout> playerControlsLinearLayout, LazyViewFinder<LinearLayout> menuControlsLinearLayout, LazyViewFinder<RatingBar> ratingBar) {
        this.playerControlsLinearLayout = playerControlsLinearLayout;
        this.menuControlsLinearLayout = menuControlsLinearLayout;
        this.ratingBar = ratingBar;
    }

    void toggleVisibility(boolean isVisible) {
        this.isVisible = isVisible;

        final int normalVisibility = isVisible ? View.VISIBLE : View.INVISIBLE;

        playerControlsLinearLayout.findView().setVisibility(normalVisibility);
        ratingBar.findView().setVisibility(normalVisibility);
        menuControlsLinearLayout.findView().setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    public boolean isVisible() {
        return isVisible;
    }
}
