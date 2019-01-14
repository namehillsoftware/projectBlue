package com.lasthopesoftware.bluewater.client.stored.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoredSyncService extends IntentService {

	private static final Logger logger = LoggerFactory.getLogger(StoredSyncService.class);

	private static final String doSyncAction = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService.class, "doSyncAction");
	private static final String cancelSyncAction = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService.class, "cancelSyncAction");

	private static final int notificationId = 23;

	private static volatile boolean isSyncRunning;

	public static boolean isSyncRunning() {
		return isSyncRunning;
	}

	public static void doSync(Context context) {
		final Intent intent = new Intent(context, StoredSyncService.class);
		intent.setAction(doSyncAction);

		safelyStartService(context, intent);
	}

	public static void cancelSync(Context context) {
		final Intent intent = new Intent(context, StoredSyncService.class);
		intent.setAction(cancelSyncAction);

		safelyStartService(context, intent);
	}

	private static void safelyStartService(Context context, Intent intent) {
		try {
			context.startService(intent);
		} catch (IllegalStateException e) {
			logger.warn("An illegal state exception occurred while trying to start the service", e);
		} catch (SecurityException e) {
			logger.warn("A security exception occurred while trying to start the service", e);
		}
	}

	public StoredSyncService() {
		super(StoredSyncService.class.getName());
	}

	@Override
	protected void onHandleIntent(@Nullable Intent intent) {

	}
}
