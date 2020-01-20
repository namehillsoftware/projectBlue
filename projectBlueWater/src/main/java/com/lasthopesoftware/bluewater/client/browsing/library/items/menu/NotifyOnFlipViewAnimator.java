package com.lasthopesoftware.bluewater.client.browsing.library.items.menu;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ViewAnimator;

/**
 * Created by david on 10/28/15.
 */
public class NotifyOnFlipViewAnimator extends ViewAnimator {

    private OnViewChangedListener onViewChangedListener;

    public NotifyOnFlipViewAnimator(Context context) {
        super(context);
    }

    public NotifyOnFlipViewAnimator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setDisplayedChild(final int whichChild) {
        super.setDisplayedChild(whichChild);

        onViewChangedListener.onViewChanged(this);
    }

    public void setViewChangedListener(OnViewChangedListener onViewChangedListener) {
        this.onViewChangedListener = onViewChangedListener;
    }
}
