package com.lasthopesoftware.bluewater.client.stored.worker.constraints;

import android.content.SharedPreferences;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import com.lasthopesoftware.bluewater.ApplicationConstants;

public class SyncWorkerConstraints implements ConstrainSyncWork {

	private final SharedPreferences sharedPreferences;

	public SyncWorkerConstraints(SharedPreferences sharedPreferences) {
		this.sharedPreferences = sharedPreferences;
	}

	@Override
	public Constraints getCurrentConstraints() {
		final boolean isSyncOnWifiOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, false);
		final Constraints.Builder builder = new Constraints.Builder();
		return builder
			.setRequiredNetworkType(isSyncOnWifiOnly ? NetworkType.UNMETERED : NetworkType.CONNECTED)
			.setRequiresCharging(sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, false))
			.build();
	}
}
