package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.access.OnGetFileStringListForClickCompleteListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.access.OnGetFileStringListForClickErrorListener
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response

class PlayClickHandler(
    menuContainer: NotifyOnFlipViewAnimator,
    private val fileListParameterProvider: IFileListParameterProvider,
	private val fileStringListProvider: FileStringListProvider,
    private val item: Item
) : AbstractMenuClickHandler(menuContainer) {
    override fun onClick(v: View) {
		fileStringListProvider
			.promiseFileStringList(FileListParameters.Options.None, *fileListParameterProvider.getFileListParameters(item))
            .then(OnGetFileStringListForClickCompleteListener(v.context))
            .excuse(OnGetFileStringListForClickErrorListener(v, this))
            .eventuallyExcuse(
                response(
                    UnexpectedExceptionToasterResponse(v.context),
                    v.context
                )
            )
        super.onClick(v)
    }
}
