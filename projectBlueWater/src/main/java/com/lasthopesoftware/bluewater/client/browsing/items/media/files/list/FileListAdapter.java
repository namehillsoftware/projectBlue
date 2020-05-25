package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemMenuBuilder;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.INowPlayingFileProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler;

import java.util.List;

class FileListAdapter extends AbstractFileListAdapter {

    private final FileListItemMenuBuilder fileListItemMenuBuilder;

	FileListAdapter(Context context, int resource, List<ServiceFile> serviceFiles, IItemListMenuChangeHandler itemListMenuChangeHandler, INowPlayingFileProvider nowPlayingFileProvider) {
		super(context, resource, serviceFiles);

        final ViewChangedHandler viewChangedHandler = new ViewChangedHandler();
        viewChangedHandler.setOnViewChangedListener(itemListMenuChangeHandler);
        viewChangedHandler.setOnAnyMenuShown(itemListMenuChangeHandler);
        viewChangedHandler.setOnAllMenusHidden(itemListMenuChangeHandler);

        fileListItemMenuBuilder = new FileListItemMenuBuilder(serviceFiles, nowPlayingFileProvider);
        fileListItemMenuBuilder.setOnViewChangedListener(viewChangedHandler);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return fileListItemMenuBuilder.getView(position, getItem(position), convertView, parent);
    }
}
