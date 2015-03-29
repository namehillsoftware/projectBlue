package com.lasthopesoftware.bluewater.servers.library.items.menu;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ViewFlipper;

/***
 * Will flip a menu item if it is a ViewFlipper class and will set all sibling views to previous view
 * if they are ViewFlippers as well
 * @author david
 *
 */
public class LongClickViewFlipListener implements OnItemLongClickListener {

    private OnViewFlippedListener mOnViewFlippedListener;

    private ViewFlipper mFlippedView;

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        tryFlipToPreviousView(mFlippedView);

		if (view instanceof ViewFlipper) {
			final ViewFlipper parentView = (ViewFlipper)view;
			parentView.showNext();
            mFlippedView = parentView;
            if (mOnViewFlippedListener != null)
                mOnViewFlippedListener.onViewFlipped(mFlippedView);
			return true;
		}
		return false;
	}

    public void setOnViewFlipped(OnViewFlippedListener onViewFlippedListener) {
        mOnViewFlippedListener = onViewFlippedListener;
    }

    public final static boolean tryFlipToPreviousView(final ViewFlipper flippedView) {
        if (flippedView == null || flippedView.getDisplayedChild() == 0) return false;

        flippedView.showPrevious();
        return true;
    }
}
