package com.lasthopesoftware.bluewater.client.connection;

import android.app.AlertDialog;
import android.content.Context;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.helpers.PollConnection;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

public class WaitForConnectionDialog {

	private static CreateAndHold<Runnable> lazyDismissalInstance = new AbstractSynchronousLazy<Runnable>() {
		@Override
		protected Runnable create() {
			return () -> {
				if (instance.isShowing()) instance.dismiss();
			};
		}
	};

	private static AlertDialog instance = null;
	
	public synchronized static AlertDialog show(final Context context) {
		if (instance != null && instance.isShowing()) return instance;

		final String message = String.format(context.getString(R.string.lbl_attempting_to_reconnect), context.getString(R.string.app_name));
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);
		builder.setTitle(context.getText(R.string.lbl_connection_lost_title)).setMessage(message).setCancelable(true);
		builder.setNegativeButton(context.getText(R.string.btn_cancel), (dialog, which) -> {
			PollConnection.Instance.get(context).stopPolling();
			dialog.dismiss();
		});

		instance = builder.create();
		instance.setOnShowListener(dialog -> {
			PollConnection.Instance.get(context).addOnConnectionRegainedListener(lazyDismissalInstance.getObject());

			PollConnection.Instance.get(context).addOnPollingCancelledListener(lazyDismissalInstance.getObject());

			instance.setOnDismissListener(dialogInterface -> {
				PollConnection.Instance.get(context).removeOnConnectionRegainedListener(lazyDismissalInstance.getObject());
				PollConnection.Instance.get(context).removeOnPollingCancelledListener(lazyDismissalInstance.getObject());
			});

			instance.setOnCancelListener(dialogInterface -> {
				PollConnection.Instance.get(context).removeOnConnectionRegainedListener(lazyDismissalInstance.getObject());
				PollConnection.Instance.get(context).removeOnPollingCancelledListener(lazyDismissalInstance.getObject());
			});

			PollConnection.Instance.get(context).startPolling();
		});

		instance.show();

		return instance;
	}
}
