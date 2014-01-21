package com.lasthopesoftware.bluewater.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.access.connection.PollConnectionTask;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class WaitForConnectionDialog {

	private static AlertDialog _instance = null;
	
	public synchronized static AlertDialog show(Context context) {
		if (_instance != null && _instance.isShowing()) return _instance;
		
		String title = context.getString(R.string.lbl_connection_lost_title);
		String message = String.format(context.getString(R.string.lbl_waiting_for_connection), context.getString(R.string.app_name));
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title).setMessage(message).setCancelable(true);
		builder.setNegativeButton("Cancel", new OnClickListener() {
			
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
