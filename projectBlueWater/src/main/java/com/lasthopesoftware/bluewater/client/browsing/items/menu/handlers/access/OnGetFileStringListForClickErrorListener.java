package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.access;

import android.view.View;

import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter;
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService;
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionDialog;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

public final class OnGetFileStringListForClickErrorListener implements ImmediateResponse<Throwable, Void> {
    private final View mView;
    private final View.OnClickListener mOnClickListener;

    public OnGetFileStringListForClickErrorListener(final View view, final View.OnClickListener onClickListener) {
        mView = view;
        mOnClickListener = onClickListener;
    }

    @Override
    public Void respond(Throwable innerException) throws Throwable {
        if (ConnectionLostExceptionFilter.isConnectionLostException(innerException)) {
			WaitForConnectionDialog.show(mView.getContext());
            PollConnectionService.pollSessionConnection(mView.getContext())
                .then(c -> {
                    mOnClickListener.onClick(mView);
                    return null;
                });
            return null;
        }

        throw innerException;
    }

}
