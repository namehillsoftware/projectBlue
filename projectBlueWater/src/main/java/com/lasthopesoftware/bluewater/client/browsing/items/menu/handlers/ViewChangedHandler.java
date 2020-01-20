package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers;

import android.widget.ViewAnimator;

import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.OnAllMenusHidden;
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.OnAnyMenuShown;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.OnViewChangedListener;

/**
 * Created by david on 11/2/15.
 */
public class ViewChangedHandler implements OnViewChangedListener {

    private ViewAnimator shownMenu;

    private OnViewChangedListener onViewChangedListener;
    private OnAnyMenuShown onAnyMenuShown;

    private OnAllMenusHidden onAllMenusHidden;

    private int numberOfMenusShown;

    @Override
    public void onViewChanged(ViewAnimator viewAnimator) {

        if (viewAnimator.getDisplayedChild() > 0) {
            if (numberOfMenusShown == 0 && onAnyMenuShown != null) onAnyMenuShown.onAnyMenuShown();

            ++numberOfMenusShown;

            if (shownMenu != null)
                LongClickViewAnimatorListener.tryFlipToPreviousView(shownMenu);

            shownMenu = viewAnimator;
        } else {

            if (shownMenu == viewAnimator) shownMenu = null;

            if (--numberOfMenusShown == 0 && onAllMenusHidden != null) onAllMenusHidden.onAllMenusHidden();
        }

        if (onViewChangedListener != null)
            onViewChangedListener.onViewChanged(viewAnimator);
    }


    public void setOnViewChangedListener(OnViewChangedListener onViewChangedListener) {
        this.onViewChangedListener = onViewChangedListener;
    }

    public void setOnAnyMenuShown(OnAnyMenuShown onAnyMenuShown) {
        this.onAnyMenuShown = onAnyMenuShown;
    }

    public void setOnAllMenusHidden(OnAllMenusHidden onAllMenusHidden) {
        this.onAllMenusHidden = onAllMenusHidden;
    }
}
