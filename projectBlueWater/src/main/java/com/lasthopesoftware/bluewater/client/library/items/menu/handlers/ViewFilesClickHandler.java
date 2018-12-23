package com.lasthopesoftware.bluewater.client.library.items.menu.handlers;

import android.view.View;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.media.files.list.FileListActivity;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;

/**
 * Created by david on 4/3/15.
 */
public final class ViewFilesClickHandler extends AbstractMenuClickHandler {
    private final IItem item;

    public ViewFilesClickHandler(NotifyOnFlipViewAnimator menuContainer, IItem item) {
        super(menuContainer);
        this.item = item;
    }

    @Override
    public void onClick(View v) {
        FileListActivity.startFileListActivity(v.getContext(), item);
        super.onClick(v);
    }
}