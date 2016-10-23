package com.lasthopesoftware.bluewater.client.library.views.handlers;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.playlists.ClickPlaylistListener;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;

import java.util.List;

/**
 * Created by david on 11/5/15.
 */
public class OnGetLibraryViewPlaylistResultsComplete extends OnGetLibraryViewIItemResultsComplete<Playlist> {

    private final ListView listView;
    private final Activity activity;

    public OnGetLibraryViewPlaylistResultsComplete(Activity activity, ViewGroup container, ListView listView, View loadingView, int position, IItemListMenuChangeHandler itemListMenuChangeHandler) {
        super(activity, container, listView, loadingView, position, itemListMenuChangeHandler);

        this.listView = listView;
        this.activity = activity;
    }

    @Override
    public void runWith(List<Playlist> result) {
        super.runWith(result);
        if (result == null) return;

        listView.setOnItemClickListener(new ClickPlaylistListener(activity, result));
    }
}
