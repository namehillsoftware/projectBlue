package com.lasthopesoftware.bluewater.activities.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class WaitForConnectionDialog {

	private static AlertDialog _instance = null;
	
	public synchronized static AlertDialog show(Context context) {
		if (_instance != null && _instance.isShowing()) return _instance;
		
		final String message = String.format(context.getString(R.string.lbl_attempting_to_reconnect), context.getString(R.string.app_name));
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getText(R.string.lbl_connection_lost_title)).setMessage(message).setCancelable(true);
		builder.setNegativeButton(context.getText(R.string.btn_cancel), new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		
		builder.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				PollConnectionTask.Instance.get().stopPolling();
			}
		});
		
		_instance = builder.show();
					
		PollConnectionTask.Instance.get().addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
				_instance.dismiss();
			}
		});
		
		PollConnectionTask.Instance.get().startPolling();
		
		return _instance;
	}
}
