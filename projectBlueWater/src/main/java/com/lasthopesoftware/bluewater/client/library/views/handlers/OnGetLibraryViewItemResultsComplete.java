package com.lasthopesoftware.bluewater.client.library.views.handlers;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.list.ClickItemListener;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 11/5/15.
 */
public class OnGetLibraryViewItemResultsComplete extends OnGetLibraryViewIItemResultsComplete<Item> {
    private final ListView listView;
    private final Activity activity;

    public OnGetLibraryViewItemResultsComplete(Activity activity, ViewGroup container, ListView listView, View loadingView, int position, IItemListMenuChangeHandler itemListMenuChangeHandler) {
        super(activity, container, listView, loadingView, position, itemListMenuChangeHandler);

        this.listView = listView;
        this.activity = activity;
    }

    @Override
    public void run(List<Item> result) {
        super.run(result);
        if (result == null) return;

        listView.setOnItemClickListener(new ClickItemListener(activity, result instanceof ArrayList ? (ArrayList<Item>) result : new ArrayList<>(result)));
    }
}
