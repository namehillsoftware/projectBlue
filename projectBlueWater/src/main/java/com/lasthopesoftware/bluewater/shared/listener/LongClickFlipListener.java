package com.lasthopesoftware.bluewater.shared.listener;

import android.view.KeyEvent;
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
public class LongClickFlipListener implements OnItemLongClickListener {

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			final View child = parent.getChildAt(i);
			if (child instanceof ViewFlipper) {
				final ViewFlipper flipper = ((ViewFlipper)child); 
				if (flipper.getDisplayedChild() == 0) continue;
				
				flipper.showPrevious();
			}
		}
		if (view instanceof ViewFlipper) {
			final ViewFlipper parentView = (ViewFlipper)view;
			parentView.showNext();
            parentView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode != KeyEvent.KEYCODE_BACK) return false;

                    parentView.setOnKeyListener(null);
                    if (parentView.getDisplayedChild() == 0) return false;

                    parentView.showPrevious();
                    return true;
                }
            });
			return true;
		}
		return false;
	}

}
