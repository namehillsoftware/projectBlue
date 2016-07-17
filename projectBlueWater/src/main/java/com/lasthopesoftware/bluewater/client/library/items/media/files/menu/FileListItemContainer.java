package com.lasthopesoftware.bluewater.client.library.items.media.files.menu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.shared.view.LazyViewFinder;

/**
 * Created by david on 4/14/15.
 */
public class FileListItemContainer {

    private final RelativeLayout textContainer;
    private final LazyViewFinder<TextView> textViewFinder;
    private final NotifyOnFlipViewAnimator notifyOnFlipViewAnimator;

    public FileListItemContainer(Context parentContext) {
        notifyOnFlipViewAnimator = new NotifyOnFlipViewAnimator(parentContext);

        notifyOnFlipViewAnimator.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final LayoutInflater inflater = (LayoutInflater) parentContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        textContainer = (RelativeLayout) inflater.inflate(R.layout.layout_standard_text, notifyOnFlipViewAnimator, false);
        textViewFinder = new LazyViewFinder<>(textContainer, R.id.tvStandard);

        notifyOnFlipViewAnimator.addView(textContainer);
    }

    public RelativeLayout getTextViewContainer() {
        return textContainer;
    }

    public TextView getTextViewFinder() {
        return textViewFinder.findView();
    }

    public NotifyOnFlipViewAnimator getViewAnimator() {
        return notifyOnFlipViewAnimator;
    }
}
