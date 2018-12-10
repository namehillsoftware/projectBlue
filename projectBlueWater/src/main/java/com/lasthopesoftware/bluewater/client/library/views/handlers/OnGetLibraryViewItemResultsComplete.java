package com.lasthopesoftware.bluewater.client.library.views.handlers;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.list.ClickItemListener;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 11/5/15.
 */
public class OnGetLibraryViewItemResultsComplete extends OnGetLibraryViewIItemResultsComplete<Item> {
    private final ListView listView;
    private final Activity activity;

    public OnGetLibraryViewItemResultsComplete(Activity activity, ViewGroup container, ListView listView, View loadingView, int position, IItemListMenuChangeHandler itemListMenuChangeHandler, IFileListParameterProvider fileListParameterProvider, StoredItemAccess storedItemAccess, Library library) {
        super(activity, container, listView, loadingView, position, itemListMenuChangeHandler, fileListParameterProvider, storedItemAccess, library);

        this.listView = listView;
        this.activity = activity;
    }

    @Override
    public Void respond(List<Item> result) {
        super.respond(result);
        if (result != null)
            listView.setOnItemClickListener(new ClickItemListener(activity, result instanceof ArrayList ? (ArrayList<Item>) result : new ArrayList<>(result)));

        return null;
    }
}
