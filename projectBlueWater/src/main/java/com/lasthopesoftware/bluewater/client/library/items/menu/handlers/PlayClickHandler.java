package com.lasthopesoftware.bluewater.client.library.items.menu.handlers;

import android.view.View;

import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.access.OnGetFileStringListForClickCompleteListener;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.access.OnGetFileStringListForClickErrorListener;

public final class PlayClickHandler extends AbstractMenuClickHandler {
    private final IFileListParameterProvider item;

    public PlayClickHandler(NotifyOnFlipViewAnimator menuContainer, IFileListParameterProvider item) {
        super(menuContainer);

	    this.item = item;
    }

    @Override
    public void onClick(final View v) {
	    (new FileStringListProvider(SessionConnection.getSessionConnectionProvider()))
				.promiseFileStringList(item)
			    .next(new OnGetFileStringListForClickCompleteListener(v.getContext()))
			    .error(new OnGetFileStringListForClickErrorListener(v, this));

        super.onClick(v);
    }
}