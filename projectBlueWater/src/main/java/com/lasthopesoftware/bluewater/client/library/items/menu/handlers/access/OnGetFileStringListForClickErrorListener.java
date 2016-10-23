package com.lasthopesoftware.bluewater.client.library.items.menu.handlers.access;

import android.view.View;

import com.lasthopesoftware.bluewater.client.connection.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.client.connection.helpers.PollConnection;
import com.vedsoft.futures.callables.OneParameterFunction;

import java.io.IOException;

/**
 * Created by david on 4/3/15.
 */
public class OnGetFileStringListForClickErrorListener implements OneParameterFunction<Exception, Boolean> {
    private final View mView;
    private final View.OnClickListener mOnClickListener;

    public OnGetFileStringListForClickErrorListener(final View view, final View.OnClickListener onClickListener) {
        mView = view;
        mOnClickListener = onClickListener;
    }

    @Override
    public Boolean expectUsing(Exception innerException) {
        if (innerException instanceof IOException) {
            PollConnection.Instance.get(mView.getContext()).addOnConnectionRegainedListener(() -> mOnClickListener.onClick(mView));

            WaitForConnectionDialog.show(mView.getContext());
            return true;
        }
        return false;
    }

}
