package com.lasthopesoftware.bluewater.client.library.items.media.files.list;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.menu.FileListItemMenuBuilder;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.ViewChangedHandler;

import java.util.List;

class FileListAdapter extends AbstractFileListAdapter {

    private final FileListItemMenuBuilder fileListItemMenuBuilder;

	public FileListAdapter(Context context, int resource, List<IFile> files, IItemListMenuChangeHandler itemListMenuChangeHandler) {
		super(context, resource, files);

        final ViewChangedHandler viewChangedHandler = new ViewChangedHandler();
        viewChangedHandler.setOnViewChangedListener(itemListMenuChangeHandler);
        viewChangedHandler.setOnAnyMenuShown(itemListMenuChangeHandler);
        viewChangedHandler.setOnAllMenusHidden(itemListMenuChangeHandler);

        fileListItemMenuBuilder = new FileListItemMenuBuilder(files);
        fileListItemMenuBuilder.setOnViewChangedListener(viewChangedHandler);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return fileListItemMenuBuilder.getView(position, getItem(position), convertView, parent);
    }
}