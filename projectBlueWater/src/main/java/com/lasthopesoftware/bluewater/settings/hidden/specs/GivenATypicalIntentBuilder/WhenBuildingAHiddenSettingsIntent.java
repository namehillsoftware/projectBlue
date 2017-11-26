package com.lasthopesoftware.bluewater.settings.hidden.specs.GivenATypicalIntentBuilder;

import android.content.Intent;

import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsActivity;
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsActivityIntentBuilder;
import com.lasthopesoftware.resources.intents.IntentFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class WhenBuildingAHiddenSettingsIntent {

	private static Intent intent;

	@Before
	public void before() {
		final HiddenSettingsActivityIntentBuilder hiddenSettingsActivityIntentBuilder =
			new HiddenSettingsActivityIntentBuilder(new IntentFactory(RuntimeEnvironment.application));

		intent = hiddenSettingsActivityIntentBuilder.buildHiddenSettingsIntent();
	}

	@Test
	public void thenAHiddenSettingsActivityIntentIsReturned() {
		assertThat(intent.getComponent().getClassName()).isEqualTo(HiddenSettingsActivity.class.getName());
	}
}
