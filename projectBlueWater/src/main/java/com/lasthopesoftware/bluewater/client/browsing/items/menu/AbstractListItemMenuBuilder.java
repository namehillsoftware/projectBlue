package com.lasthopesoftware.bluewater.client.browsing.items.menu;

import android.view.ViewGroup;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.BaseMenuViewHolder;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;

/**
 * Created by david on 11/7/15.
 */
public abstract class AbstractListItemMenuBuilder<T, ViewHolder extends BaseMenuViewHolder> {

    private OnViewChangedListener onViewChangedListener;

    protected final OnViewChangedListener getOnViewChangedListener() {
        return onViewChangedListener;
    }

    public final void setOnViewChangedListener(OnViewChangedListener onViewChangedListener) {
        this.onViewChangedListener = onViewChangedListener;
    }

    public abstract ViewHolder newViewHolder(ViewGroup parent);

    public abstract void setupView(ViewHolder viewHolder, PositionedFile positionedFile);
}
