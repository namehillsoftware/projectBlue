package com.lasthopesoftware.bluewater.client.library.views.handlers;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.playlists.ClickPlaylistListener;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

import java.util.List;

public class OnGetLibraryViewPlaylistResultsComplete extends OnGetLibraryViewIItemResultsComplete<Playlist> {

    private final ListView listView;
    private final Activity activity;

    public OnGetLibraryViewPlaylistResultsComplete(Activity activity, ViewGroup container, ListView listView, View loadingView, int position, IItemListMenuChangeHandler itemListMenuChangeHandler, StoredItemAccess storedItemAccess, Library library) {
        super(activity, container, listView, loadingView, position, itemListMenuChangeHandler, storedItemAccess, library);

        this.listView = listView;
        this.activity = activity;
    }

    @Override
    public Void respond(List<Playlist> result) {
        super.respond(result);

        if (result != null)
            listView.setOnItemClickListener(new ClickPlaylistListener(activity, result));

        return null;
    }
}
