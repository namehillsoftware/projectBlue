package com.lasthopesoftware.bluewater.sync.constraints.specs.GivenPowerOnlyIsSet;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.Constraints;
import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.sync.constraints.SyncWorkerConstraints;
import com.lasthopesoftware.specs.AndroidContext;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenGettingConstraints extends AndroidContext {

	private static Constraints constraints;

	@Override
	public void before() {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext());
		sharedPreferences.edit()
			.putBoolean(ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, true)
			.apply();
		final SyncWorkerConstraints syncWorkerConstraints = new SyncWorkerConstraints(sharedPreferences);
		constraints = syncWorkerConstraints.getCurrentConstraints();
	}

	@Test
	public void thenTheConstraintsAreCorrect() {
		assertThat(constraints.requiresCharging()).isTrue();
	}
}
