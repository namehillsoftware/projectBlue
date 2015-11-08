package com.lasthopesoftware.bluewater.servers.connection;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection;

public class WaitForConnectionDialog {

	private static AlertDialog _instance = null;
	
	public synchronized static AlertDialog show(Context context) {
		if (_instance != null && _instance.isShowing()) return _instance;
		
		final Context _context = context;
		final String message = String.format(_context.getString(R.string.lbl_attempting_to_reconnect), _context.getString(R.string.app_name));
		final AlertDialog.Builder builder = new AlertDialog.Builder(_context);
		builder.setCancelable(false);
		builder.setTitle(context.getText(R.string.lbl_connection_lost_title)).setMessage(message).setCancelable(true);
		builder.setNegativeButton(_context.getText(R.string.btn_cancel), new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				PollConnection.Instance.get(_context).stopPolling();
				dialog.dismiss();
			}
		});

		_instance = builder.create();
		_instance.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				PollConnection.Instance.get(_context).addOnConnectionRegainedListener(new Runnable() {

					@Override
					public void run() {
						if (_instance.isShowing()) _instance.dismiss();
					}
				});

				PollConnection.Instance.get(_context).addOnPollingCancelledListener(new Runnable() {

					@Override
					public void run() {
						if (_instance.isShowing()) _instance.dismiss();
					}
				});

				PollConnection.Instance.get(_context).startPolling();

			}
		});

		_instance.show();

		return _instance;
	}
}
