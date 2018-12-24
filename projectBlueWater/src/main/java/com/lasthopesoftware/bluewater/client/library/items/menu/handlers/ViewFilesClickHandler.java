package com.lasthopesoftware.bluewater.client.library.items.menu.handlers;

import android.view.View;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;

import static com.lasthopesoftware.bluewater.client.library.items.media.files.list.FileListActivity.startFileListActivity;

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
        startFileListActivity(v.getContext(), item);
        super.onClick(v);
    }
}