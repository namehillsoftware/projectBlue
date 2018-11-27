package com.lasthopesoftware.bluewater.client.settings.specs;

import android.content.Intent;
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivity;
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivityIntentBuilder;
import com.lasthopesoftware.resources.intents.IntentFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

public class GivenALibraryId {

	@RunWith(RobolectricTestRunner.class)
	public static class WhenBuildingTheEditClientSettingsActivityIntent {
		private static Intent returnedIntent;

		@Before
		public void before() {
			final EditClientSettingsActivityIntentBuilder editClientSettingsActivityIntentBuilder =
				new EditClientSettingsActivityIntentBuilder(
					new IntentFactory(RuntimeEnvironment.application));

			returnedIntent = editClientSettingsActivityIntentBuilder.buildIntent(13);
		}

		@Test
		public void thenTheIdInTheIntentIsSetToTheLibraryId() {
			assertThat(returnedIntent.getIntExtra(EditClientSettingsActivity.serverIdExtra, -1)).isEqualTo(13);
		}

		@Test
		public void thenTheReturnedIntentIsEditClientSettingsActivity() {
			assertThat(returnedIntent.getComponent().getClassName()).isEqualTo(EditClientSettingsActivity.class.getName());
		}
	}
}
