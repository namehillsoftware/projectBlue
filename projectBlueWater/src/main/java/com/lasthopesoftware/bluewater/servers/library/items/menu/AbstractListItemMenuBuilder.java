package com.lasthopesoftware.bluewater.servers.library.items.menu;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by david on 11/7/15.
 */
public abstract class AbstractListItemMenuBuilder<T> {

    private OnViewChangedListener onViewChangedListener;

    public final OnViewChangedListener getOnViewChangedListener() {
        return onViewChangedListener;
    }

    public final void setOnViewChangedListener(OnViewChangedListener onViewChangedListener) {
        this.onViewChangedListener = onViewChangedListener;
    }

    public abstract View getView(int position, T item, View convertView, ViewGroup parent);
}
