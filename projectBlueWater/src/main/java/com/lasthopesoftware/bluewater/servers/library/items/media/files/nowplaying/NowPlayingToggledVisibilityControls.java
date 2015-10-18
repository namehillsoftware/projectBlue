package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.RatingBar;

/**
 * Created by david on 10/16/15.
 */
public class NowPlayingToggledVisibilityControls {
    private final LinearLayout playerControlsLinearLayout;
    private final LinearLayout menuControlsLinearLayout;
    private final RatingBar ratingBar;

    private boolean isVisible = true;

    public NowPlayingToggledVisibilityControls(LinearLayout playerControlsLinearLayout, LinearLayout menuControlsLinearLayout, RatingBar ratingBar) {
        this.playerControlsLinearLayout = playerControlsLinearLayout;
        this.menuControlsLinearLayout = menuControlsLinearLayout;
        this.ratingBar = ratingBar;
    }

    public void toggleVisibility(boolean isVisible) {
        this.isVisible = isVisible;

        final int normalVisibility = isVisible ? View.VISIBLE : View.INVISIBLE;

        playerControlsLinearLayout.setVisibility(normalVisibility);
        ratingBar.setVisibility(normalVisibility);
        menuControlsLinearLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    public boolean isVisible() {
        return isVisible;
    }
}
