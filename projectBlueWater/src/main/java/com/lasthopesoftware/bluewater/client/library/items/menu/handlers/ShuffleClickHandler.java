package com.lasthopesoftware.bluewater.client.library.items.menu.handlers;

import android.view.View;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.access.OnGetFileStringListForClickCompleteListener;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.access.OnGetFileStringListForClickErrorListener;

public final class ShuffleClickHandler extends AbstractMenuClickHandler {

    private final IFileListParameterProvider item;

    public ShuffleClickHandler(NotifyOnFlipViewAnimator menuContainer, IFileListParameterProvider item) {
        super(menuContainer);
	    this.item = item;
    }

    @Override
    public void onClick(View v) {
		SessionConnection.getInstance(v.getContext()).promiseSessionConnection()
			.then(FileStringListProvider::new)
			.eventually(p -> p.promiseFileStringList(FileListParameters.Options.Shuffled, item.getFileListParameters()))
			.then(new OnGetFileStringListForClickCompleteListener(v.getContext()))
			.excuse(new OnGetFileStringListForClickErrorListener(v, this));

        super.onClick(v);
    }
}