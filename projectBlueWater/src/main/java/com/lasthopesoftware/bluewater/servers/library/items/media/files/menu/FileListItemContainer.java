package com.lasthopesoftware.bluewater.servers.library.items.media.files.menu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;

/**
 * Created by david on 4/14/15.
 */
public class FileListItemContainer {

    private final RelativeLayout mTextContainer;
    private final TextView mTextView;
    private final NotifyOnFlipViewAnimator notifyOnFlipViewAnimator;

    public FileListItemContainer(Context parentContext) {
        notifyOnFlipViewAnimator = new NotifyOnFlipViewAnimator(parentContext);

        notifyOnFlipViewAnimator.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final LayoutInflater inflater = (LayoutInflater) parentContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mTextContainer = (RelativeLayout) inflater.inflate(R.layout.layout_standard_text, notifyOnFlipViewAnimator, false);
        mTextView = (TextView) mTextContainer.findViewById(R.id.tvStandard);
        mTextView.setMarqueeRepeatLimit(1);

        notifyOnFlipViewAnimator.addView(mTextContainer);
    }

    public RelativeLayout getTextViewContainer() {
        return mTextContainer;
    }

    public TextView getTextView() {
        return mTextView;
    }

    public NotifyOnFlipViewAnimator getViewAnimator() {
        return notifyOnFlipViewAnimator;
    }
}
