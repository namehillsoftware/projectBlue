package com.lasthopesoftware.bluewater.settings.hidden.specs.GivenATypicalIntentBuilder;

import android.content.Intent;

import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsActivity;
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsActivityIntentBuilder;
import com.lasthopesoftware.resources.intents.specs.FakeIntentFactory;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenBuildingAHiddenSettingsIntent {

	private static Intent intent;

	@BeforeClass
	public static void before() {
		final HiddenSettingsActivityIntentBuilder hiddenSettingsActivityIntentBuilder =
			new HiddenSettingsActivityIntentBuilder(new FakeIntentFactory());

		intent = hiddenSettingsActivityIntentBuilder.buildHiddenSettingsIntent();
	}

	@Test
	public void thenAHiddenSettingsActivityIntentIsReturned() {
		assertThat(intent.getComponent().getClassName()).isEqualTo(HiddenSettingsActivity.class.getName());
	}
}
