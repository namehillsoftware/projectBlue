package com.lasthopesoftware.bluewater.client.connection;

import android.app.AlertDialog;
import android.content.Context;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService;
import com.namehillsoftware.handoff.promises.Promise;
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
	
	public synchronized static Promise<AlertDialog> show(final Context context) {
		if (instance != null && instance.isShowing()) return new Promise<>(instance);

		final String message = String.format(context.getString(R.string.lbl_attempting_to_reconnect), context.getString(R.string.app_name));
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);
		builder.setTitle(context.getText(R.string.lbl_connection_lost_title)).setMessage(message).setCancelable(true);

		return PollConnectionService.Instance.promise(context)
			.then(pollConnectionService -> {
				builder.setNegativeButton(context.getText(R.string.btn_cancel), (dialog, which) -> {
					pollConnectionService.stopPolling();
					dialog.dismiss();
				});

				instance = builder.create();
				instance.setOnShowListener(dialog -> {
					pollConnectionService.addOnConnectionRegainedListener(lazyDismissalInstance.getObject());

					pollConnectionService.addOnPollingCancelledListener(lazyDismissalInstance.getObject());

					instance.setOnDismissListener(dialogInterface -> {
						pollConnectionService.removeOnConnectionRegainedListener(lazyDismissalInstance.getObject());
						pollConnectionService.removeOnPollingCancelledListener(lazyDismissalInstance.getObject());
					});

					instance.setOnCancelListener(dialogInterface -> {
						pollConnectionService.removeOnConnectionRegainedListener(lazyDismissalInstance.getObject());
						pollConnectionService.removeOnPollingCancelledListener(lazyDismissalInstance.getObject());
					});

					pollConnectionService.startPolling();
				});

				instance.show();
				return instance;
			});
	}
}
