package com.lasthopesoftware.bluewater.client.library.items.menu.handlers;

import android.view.View;

import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;

/**
 * Created by david on 3/31/15.
 */
public abstract class AbstractMenuClickHandler implements View.OnClickListener {
    private final NotifyOnFlipViewAnimator menuContainer;

    public AbstractMenuClickHandler(NotifyOnFlipViewAnimator menuContainer) {
        this.menuContainer = menuContainer;
    }

    @Override
    public void onClick(View v) {
        if (menuContainer.getDisplayedChild() > 0) menuContainer.showPrevious();
    }
}
