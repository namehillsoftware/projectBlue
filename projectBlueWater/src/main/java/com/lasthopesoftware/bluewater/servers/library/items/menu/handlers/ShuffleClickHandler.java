package com.lasthopesoftware.bluewater.servers.library.items.menu.handlers;

import android.view.View;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.Files;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFilesContainer;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.access.OnGetFileStringListForClickCompleteListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.access.OnGetFileStringListForClickErrorListener;

/**
 * Created by david on 4/3/15.
 */
public final class ShuffleClickHandler extends AbstractMenuClickHandler {
    private final IFilesContainer mItem;

    public ShuffleClickHandler(ViewFlipper menuContainer, IFilesContainer item) {
        super(menuContainer);
        mItem = item;
    }

    @Override
    public void onClick(View v) {
        mItem.getFiles().getFileStringList(Files.GET_SHUFFLED, new OnGetFileStringListForClickCompleteListener(v.getContext()), new OnGetFileStringListForClickErrorListener(v, this));
        super.onClick(v);
    }
}