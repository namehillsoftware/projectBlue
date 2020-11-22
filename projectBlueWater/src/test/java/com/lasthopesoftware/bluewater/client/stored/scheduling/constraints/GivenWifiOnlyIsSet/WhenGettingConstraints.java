package com.lasthopesoftware.bluewater.client.stored.scheduling.constraints.GivenWifiOnlyIsSet;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Constraints;
import androidx.work.NetworkType;

import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.client.stored.scheduling.constraints.SyncWorkerConstraints;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenGettingConstraints extends AndroidContext {

	private static Constraints constraints;

	@Override
	public void before() {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext());
		sharedPreferences.edit()
			.putBoolean(ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, true)
			.apply();
		final SyncWorkerConstraints syncWorkerConstraints = new SyncWorkerConstraints(sharedPreferences);
		constraints = syncWorkerConstraints.getCurrentConstraints();
	}

	@Test
	public void thenTheConstraintsAreCorrect() {
		assertThat(constraints.getRequiredNetworkType()).isEqualTo(NetworkType.UNMETERED);
	}
}
