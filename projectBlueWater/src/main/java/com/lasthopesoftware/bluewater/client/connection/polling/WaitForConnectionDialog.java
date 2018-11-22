package com.lasthopesoftware.bluewater.client.connection.polling;

import android.app.AlertDialog;
import android.content.Context;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.Promise;

public class WaitForConnectionDialog {

	private static AlertHolder instance = null;
	
	public synchronized static void show(final Context context) {
		if (instance == null || !instance.isShowing()) instance = new AlertHolder(context);
	}

	private static class AlertHolder {

		private final AlertDialog alertDialog;
		private boolean isDismissed;

		AlertHolder(Context context) {

			final String message = String.format(context.getString(R.string.lbl_attempting_to_reconnect), context.getString(R.string.app_name));
			final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DialogTheme);
			builder.setCancelable(false);
			builder.setTitle(context.getText(R.string.lbl_connection_lost_title)).setMessage(message).setCancelable(true);

			Promise<IConnectionProvider> pollingSessionConnection = PollConnectionService.pollSessionConnection(context);

			builder.setNegativeButton(context.getText(R.string.btn_cancel), (dialog, which) -> {
				pollingSessionConnection.cancel();
				dialog.dismiss();
			});

			builder.setOnDismissListener(dialogInterface -> isDismissed = true);

			builder.setOnCancelListener(dialogInterface -> isDismissed = true);

			alertDialog = builder.create();

			alertDialog.setInverseBackgroundForced(true);

			alertDialog.setOnShowListener(dialog -> pollingSessionConnection
				.eventually(LoopedInPromise.response(connectionProvider -> {
					if (!isDismissed && alertDialog.isShowing()) alertDialog.dismiss();

					return null;
				}, context), LoopedInPromise.response(e -> {
					if (!isDismissed && alertDialog.isShowing()) alertDialog.dismiss();

					return null;
				}, context)));

			alertDialog.show();
		}

		boolean isShowing() {
			return alertDialog.isShowing();
		}
	}
}
