package com.lasthopesoftware.bluewater.servers.library.items.menu.handlers;

import android.view.View;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFilesContainer;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.access.OnGetFileStringListForClickCompleteListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.access.OnGetFileStringListForClickErrorListener;

/**
 * Created by david on 4/3/15.
 */
public final class PlayClickHandler extends AbstractMenuClickHandler {
    private final IFilesContainer mItem;

    public PlayClickHandler(NotifyOnFlipViewAnimator menuContainer, IFilesContainer item) {
        super(menuContainer);
        mItem = item;
    }

    @Override
    public void onClick(final View v) {
        mItem.getFiles().getFileStringList(new OnGetFileStringListForClickCompleteListener(v.getContext()), new OnGetFileStringListForClickErrorListener(v, this));
        super.onClick(v);
    }
}