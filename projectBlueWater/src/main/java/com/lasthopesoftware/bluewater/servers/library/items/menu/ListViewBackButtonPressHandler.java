package com.lasthopesoftware.bluewater.servers.library.items.menu;

import android.view.View;
import android.widget.ListView;
import android.widget.ViewFlipper;

/**
 * Created by david on 3/28/15.
 */
public class ListViewBackButtonPressHandler {
    public static boolean HandleListViewBackButtonPress(ListView listView) {
        for (int i = 0; i < listView.getChildCount(); i++) {
            final View childView = listView.getChildAt(i);
            if (childView == null || !(childView instanceof ViewFlipper)) continue;

            final ViewFlipper childViewFlipper = (ViewFlipper)childView;

            if (childViewFlipper.getDisplayedChild() == 0) continue;

            childViewFlipper.showPrevious();
            return true;
        }

        return false;
    }
}
