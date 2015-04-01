package com.lasthopesoftware.bluewater.servers.library.items.menu;

import android.view.View;
import android.widget.ViewFlipper;

/**
 * Created by david on 3/31/15.
 */
public abstract class AbstractMenuClickHandler implements View.OnClickListener {
    private final ViewFlipper mMenuContainer;

    public AbstractMenuClickHandler(ViewFlipper menuContainer) {
        mMenuContainer = menuContainer;
    }

    @Override
    public void onClick(View v) {
        if (mMenuContainer.getDisplayedChild() > 0) mMenuContainer.showPrevious();
    }
}
