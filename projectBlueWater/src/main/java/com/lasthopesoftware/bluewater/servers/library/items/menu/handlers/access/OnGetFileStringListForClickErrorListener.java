package com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.access;

import android.view.View;

import com.lasthopesoftware.bluewater.servers.connection.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection;
import com.lasthopesoftware.callables.IThreeParameterCallable;
import com.lasthopesoftware.threading.IFluentTask;

import java.io.IOException;

/**
 * Created by david on 4/3/15.
 */
public class OnGetFileStringListForClickErrorListener implements IThreeParameterCallable<IFluentTask<Void, Void, String>, Boolean, Exception, Boolean> {
    private final View mView;
    private final View.OnClickListener mOnClickListener;

    public OnGetFileStringListForClickErrorListener(final View view, final View.OnClickListener onClickListener) {
        mView = view;
        mOnClickListener = onClickListener;
    }

    @Override
    public Boolean call(IFluentTask<Void, Void, String> owner, Boolean isHandled, Exception innerException) {
        if (innerException instanceof IOException) {
            PollConnection.Instance.get(mView.getContext()).addOnConnectionRegainedListener(new Runnable() {

                @Override
                public void run() {
                    mOnClickListener.onClick(mView);
                }
            });

            WaitForConnectionDialog.show(mView.getContext());
            return true;
        }
        return false;
    }

}
