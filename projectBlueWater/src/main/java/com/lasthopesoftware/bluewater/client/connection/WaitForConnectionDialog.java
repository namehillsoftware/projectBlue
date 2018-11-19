package com.lasthopesoftware.bluewater.client.connection;

import android.app.AlertDialog;
import android.content.Context;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.concurrent.CancellationException;

public class WaitForConnectionDialog {

	private static AlertHolder instance = null;
	
	public synchronized static void show(final Context context) {
		if (instance != null && instance.isShowing()) return;

		instance = new AlertHolder(context);
	}

	private static class AlertHolder {

		private final AlertDialog alertDialog;
		private boolean isDismissed;

		AlertHolder(Context context) {

			final String message = String.format(context.getString(R.string.lbl_attempting_to_reconnect), context.getString(R.string.app_name));
			final AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setCancelable(false);
			builder.setTitle(context.getText(R.string.lbl_connection_lost_title)).setMessage(message).setCancelable(true);

			alertDialog = builder.create();

			Promise<IConnectionProvider> pollingSessionConnection = PollConnectionService.pollSessionConnection(context);

			builder.setNegativeButton(context.getText(R.string.btn_cancel), (dialog, which) -> {
				pollingSessionConnection.cancel();
				dialog.dismiss();
			});

			alertDialog.setOnDismissListener(dialogInterface -> isDismissed = true);

			alertDialog.setOnCancelListener(dialogInterface -> isDismissed = true);

			alertDialog.setOnShowListener(dialog -> pollingSessionConnection
				.eventually(LoopedInPromise.response(connectionProvider -> {
					if (!isDismissed && alertDialog.isShowing()) alertDialog.dismiss();

					return null;
				}, context), LoopedInPromise.response(e -> {
					if (e instanceof CancellationException) {
						if (!isDismissed && alertDialog.isShowing()) alertDialog.dismiss();
					}

					return null;
				}, context)));
		}

		boolean isShowing() {
			return alertDialog.isShowing();
		}
	}
}
