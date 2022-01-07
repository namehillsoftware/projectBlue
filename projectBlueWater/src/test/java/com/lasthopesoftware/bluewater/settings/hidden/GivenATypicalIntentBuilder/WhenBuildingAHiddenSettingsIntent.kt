package com.lasthopesoftware.bluewater.settings.hidden.GivenATypicalIntentBuilder

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsActivity
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsActivityIntentBuilder
import com.lasthopesoftware.resources.intents.IntentFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenBuildingAHiddenSettingsIntent {

	companion object {
		private var intent: Intent? = null
	}

    @Before
    fun before() {
        val hiddenSettingsActivityIntentBuilder =
            HiddenSettingsActivityIntentBuilder(IntentFactory(ApplicationProvider.getApplicationContext()))
        intent = hiddenSettingsActivityIntentBuilder.buildHiddenSettingsIntent()
    }

    @Test
    fun thenAHiddenSettingsActivityIntentIsReturned() {
        assertThat(intent!!.component!!.className).isEqualTo(HiddenSettingsActivity::class.java.name)
    }
}
