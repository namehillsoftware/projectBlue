package com.lasthopesoftware.bluewater.client.settings.specs;

import android.content.Intent;

import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivity;
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivityIntentBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 7/10/16.
 */
public class GivenALibraryId {
	public static class WhenBuildingTheEditClientSettingsActivityIntent {
		private int libraryId;

		@Before
		public void setUp() {
			final EditClientSettingsActivityIntentBuilder editClientSettingsActivityIntentBuilder =
				new EditClientSettingsActivityIntentBuilder(cls -> new Intent(cls.toString()));

			final Intent intent = editClientSettingsActivityIntentBuilder.buildIntent(13);
			this.libraryId = intent.getIntExtra(EditClientSettingsActivity.serverIdExtra, -1);
		}

		@Test
		public void thenTheIdInTheIntentIsTheLibraryId() {
			Assert.assertEquals(13, this.libraryId);
		}
	}
}
