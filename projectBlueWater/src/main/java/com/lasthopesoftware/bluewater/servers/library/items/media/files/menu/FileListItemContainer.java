package com.lasthopesoftware.bluewater.servers.library.items.media.files.menu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;

/**
 * Created by david on 4/14/15.
 */
public class FileListItemContainer {

    private final RelativeLayout mTextContainer;
    private final TextView mTextView;
    private final ViewFlipper mViewFlipper;

    public FileListItemContainer(Context parentContext) {
        mViewFlipper = new ViewFlipper(parentContext);

        mViewFlipper.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final LayoutInflater inflater = (LayoutInflater) parentContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mTextContainer = (RelativeLayout) inflater.inflate(R.layout.layout_standard_text, mViewFlipper, false);
        mTextView = (TextView) mTextContainer.findViewById(R.id.tvStandard);
        mTextView.setMarqueeRepeatLimit(1);

        mViewFlipper.addView(mTextContainer);
    }

    public RelativeLayout getTextViewContainer() {
        return mTextContainer;
    }

    public TextView getTextView() {
        return mTextView;
    }

    public ViewFlipper getViewFlipper() {
        return mViewFlipper;
    }
}
