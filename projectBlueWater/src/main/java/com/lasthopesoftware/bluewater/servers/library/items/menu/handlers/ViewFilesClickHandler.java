package com.lasthopesoftware.bluewater.servers.library.items.menu.handlers;

import android.content.Intent;
import android.view.View;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.list.FileListActivity;
import com.lasthopesoftware.bluewater.servers.library.items.menu.AbstractMenuClickHandler;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;

/**
 * Created by david on 4/3/15.
 */
public class ViewFilesClickHandler extends AbstractMenuClickHandler {
    private IItem mItem;

    public ViewFilesClickHandler(ViewFlipper menuContainer, IItem item) {
        super(menuContainer);
        mItem = item;
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(v.getContext(), FileListActivity.class);
        intent.setAction(mItem instanceof Playlist ? FileListActivity.VIEW_PLAYLIST_FILES : FileListActivity.VIEW_ITEM_FILES);
        intent.putExtra(FileListActivity.KEY, mItem.getKey());
        v.getContext().startActivity(intent);
        super.onClick(v);
    }
}