package com.lasthopesoftware.bluewater.client.browsing.library.items.menu.handlers;

import android.view.View;

import com.lasthopesoftware.bluewater.client.browsing.library.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.parameters.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.browsing.library.items.menu.handlers.access.OnGetFileStringListForClickCompleteListener;
import com.lasthopesoftware.bluewater.client.browsing.library.items.menu.handlers.access.OnGetFileStringListForClickErrorListener;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;

import static com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.forward;

public final class PlayClickHandler extends AbstractMenuClickHandler {
	private final IFileListParameterProvider fileListParameterProvider;
	private final Item item;

    public PlayClickHandler(NotifyOnFlipViewAnimator menuContainer, IFileListParameterProvider fileListParameterProvider, Item item) {
        super(menuContainer);
		this.fileListParameterProvider = fileListParameterProvider;

		this.item = item;
    }

    @Override
    public void onClick(final View v) {
    	SessionConnection.getInstance(v.getContext()).promiseSessionConnection()
			.then(FileStringListProvider::new)
			.eventually(p -> p.promiseFileStringList(FileListParameters.Options.None, fileListParameterProvider.getFileListParameters(item)))
			.then(new OnGetFileStringListForClickCompleteListener(v.getContext()))
			.excuse(new OnGetFileStringListForClickErrorListener(v, this))
			.excuse(forward())
			.eventually(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(v.getContext()), v.getContext()));

        super.onClick(v);
    }
}
