package com.lasthopesoftware.bluewater.client.library.items.media.files.list;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.menu.FileListItemMenuBuilder;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.INowPlayingFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.ViewChangedHandler;

import java.util.List;

class FileListAdapter extends AbstractFileListAdapter {

    private final FileListItemMenuBuilder fileListItemMenuBuilder;

	FileListAdapter(Context context, int resource, List<File> files, IItemListMenuChangeHandler itemListMenuChangeHandler, INowPlayingFileProvider nowPlayingFileProvider) {
		super(context, resource, files);

        final ViewChangedHandler viewChangedHandler = new ViewChangedHandler();
        viewChangedHandler.setOnViewChangedListener(itemListMenuChangeHandler);
        viewChangedHandler.setOnAnyMenuShown(itemListMenuChangeHandler);
        viewChangedHandler.setOnAllMenusHidden(itemListMenuChangeHandler);

        fileListItemMenuBuilder = new FileListItemMenuBuilder(files, nowPlayingFileProvider);
        fileListItemMenuBuilder.setOnViewChangedListener(viewChangedHandler);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return fileListItemMenuBuilder.getView(position, getItem(position), convertView, parent);
    }
}