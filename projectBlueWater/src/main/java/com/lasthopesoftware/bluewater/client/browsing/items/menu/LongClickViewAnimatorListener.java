package com.lasthopesoftware.bluewater.client.browsing.items.menu;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ViewAnimator;

/***
 * Will flip a menu item if it is a ViewFlipper class and will set all sibling views to previous view
 * if they are ViewFlippers as well
 * @author david
 *
 */
public class LongClickViewAnimatorListener implements OnItemLongClickListener, View.OnLongClickListener {

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (view instanceof ViewAnimator) {
			final ViewAnimator viewAnimator = (ViewAnimator)view;
			viewAnimator.showNext();

			return true;
		}
		return false;
	}

	@Override
	public boolean onLongClick(View view) {
		if (view instanceof ViewAnimator) {
			final ViewAnimator viewAnimator = (ViewAnimator)view;
			viewAnimator.showNext();

			return true;
		}
		return false;
	}

    public static boolean tryFlipToPreviousView(final ViewAnimator viewAnimator) {
        if (viewAnimator == null || viewAnimator.getDisplayedChild() == 0) return false;

        viewAnimator.showPrevious();
        return true;
    }
}
