package com.lasthopesoftware.bluewater.activities.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnPollingCancelledListener;

public class WaitForConnectionDialog {

	private static AlertDialog _instance = null;
	
	public synchronized static AlertDialog show(Context context) {
		if (_instance != null && _instance.isShowing()) return _instance;
		
		final Context _context = context;
		final String message = String.format(_context.getString(R.string.lbl_attempting_to_reconnect), _context.getString(R.string.app_name));
		final AlertDialog.Builder builder = new AlertDialog.Builder(_context);
		builder.setTitle(context.getText(R.string.lbl_connection_lost_title)).setMessage(message).setCancelable(true);
		builder.setNegativeButton(_context.getText(R.string.btn_cancel), new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		
		builder.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				PollConnection.Instance.get(_context).stopPolling();
			}
		});
		
		_instance = builder.show();
					
		PollConnection.Instance.get(_context).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
			
			@Override
			public void onConnectionRegained() {
				_instance.dismiss();
			}
		});
		
		PollConnection.Instance.get(_context).addOnPollingCancelledListener(new OnPollingCancelledListener() {
			
			@Override
			public void onPollingCancelled() {
				_instance.dismiss();
			}
		});
		
		PollConnection.Instance.get(_context).startPolling();
		
		return _instance;
	}
}
