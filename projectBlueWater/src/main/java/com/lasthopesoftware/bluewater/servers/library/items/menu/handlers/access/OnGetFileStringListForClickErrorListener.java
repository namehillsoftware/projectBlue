package com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.access;

import android.view.View;

import com.lasthopesoftware.bluewater.servers.connection.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection;
import com.lasthopesoftware.threading.IDataTask;
import com.lasthopesoftware.threading.ISimpleTask;

import java.io.IOException;

/**
 * Created by david on 4/3/15.
 */
public class OnGetFileStringListForClickErrorListener implements IDataTask.OnErrorListener<String> {
    private final View mView;
    private final View.OnClickListener mOnClickListener;

    public OnGetFileStringListForClickErrorListener(final View view, final View.OnClickListener onClickListener) {
        mView = view;
        mOnClickListener = onClickListener;
    }

    @Override
    public boolean onError(ISimpleTask<String, Void, String> owner, boolean isHandled, Exception innerException) {
        if (innerException instanceof IOException) {
            PollConnection.Instance.get(mView.getContext()).addOnConnectionRegainedListener(new PollConnection.OnConnectionRegainedListener() {

                @Override
                public void onConnectionRegained() {
                    mOnClickListener.onClick(mView);
                }
            });

            WaitForConnectionDialog.show(mView.getContext());
            return true;
        }
        return false;
    }

}
