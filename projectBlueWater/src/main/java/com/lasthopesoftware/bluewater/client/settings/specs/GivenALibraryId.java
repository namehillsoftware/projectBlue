package com.lasthopesoftware.bluewater.client.settings.specs;

import android.content.Intent;

import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivity;
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivityIntentBuilder;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 7/10/16.
 */
public class GivenALibraryId {

	public static class WhenBuildingTheEditClientSettingsActivityIntent {
		private static Intent mockIntent;
		private static Intent returnedIntent;

		@BeforeClass
		public static void setUp() {
			mockIntent = mock(Intent.class);
			final EditClientSettingsActivityIntentBuilder editClientSettingsActivityIntentBuilder =
					new EditClientSettingsActivityIntentBuilder(cls -> mockIntent);

			returnedIntent = editClientSettingsActivityIntentBuilder.buildIntent(13);
		}

		@Test
		public void thenTheIdInTheIntentIsSetToTheLibraryId() {
			verify(mockIntent).putExtra(EditClientSettingsActivity.serverIdExtra, 13);
		}

		@Test
		public void thenTheReturnedIntentIsTheMockIntent() {
			Assert.assertEquals(mockIntent, returnedIntent);
		}
	}
}
