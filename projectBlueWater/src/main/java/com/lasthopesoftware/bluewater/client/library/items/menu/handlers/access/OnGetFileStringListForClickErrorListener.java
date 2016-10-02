package com.lasthopesoftware.bluewater.client.library.items.menu.handlers.access;

import android.view.View;

import com.lasthopesoftware.bluewater.client.connection.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.client.connection.helpers.PollConnection;
import com.vedsoft.fluent.FluentSpecifiedTask;
import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.callables.TwoParameterCallable;

import java.io.IOException;

/**
 * Created by david on 4/3/15.
 */
public class OnGetFileStringListForClickErrorListener implements OneParameterCallable<Exception, Boolean> {
    private final View mView;
    private final View.OnClickListener mOnClickListener;

    public OnGetFileStringListForClickErrorListener(final View view, final View.OnClickListener onClickListener) {
        mView = view;
        mOnClickListener = onClickListener;
    }

    @Override
    public Boolean call(Exception innerException) {
        if (innerException instanceof IOException) {
            PollConnection.Instance.get(mView.getContext()).addOnConnectionRegainedListener(() -> mOnClickListener.onClick(mView));

            WaitForConnectionDialog.show(mView.getContext());
            return true;
        }
        return false;
    }

}
