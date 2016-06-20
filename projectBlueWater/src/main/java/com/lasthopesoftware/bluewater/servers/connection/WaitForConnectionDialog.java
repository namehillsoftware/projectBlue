package com.lasthopesoftware.bluewater.servers.connection;

import android.app.AlertDialog;
import android.content.Context;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection;

public class WaitForConnectionDialog {

	private static AlertDialog _instance = null;
	
	public synchronized static AlertDialog show(final Context context) {
		if (_instance != null && _instance.isShowing()) return _instance;

		final String message = String.format(context.getString(R.string.lbl_attempting_to_reconnect), context.getString(R.string.app_name));
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);
		builder.setTitle(context.getText(R.string.lbl_connection_lost_title)).setMessage(message).setCancelable(true);
		builder.setNegativeButton(context.getText(R.string.btn_cancel), (dialog, which) -> {
			PollConnection.Instance.get(context).stopPolling();
			dialog.dismiss();
		});

		_instance = builder.create();
		_instance.setOnShowListener(dialog -> {
			PollConnection.Instance.get(context).addOnConnectionRegainedListener(() -> {
				if (_instance.isShowing()) _instance.dismiss();
			});

			PollConnection.Instance.get(context).addOnPollingCancelledListener(() -> {
				if (_instance.isShowing()) _instance.dismiss();
			});

			PollConnection.Instance.get(context).startPolling();

		});

		_instance.show();

		return _instance;
	}
}
