package com.lasthopesoftware.bluewater.settings.hidden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text

class HiddenSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

		setContent {
			Column {
				Text("Congratulations, you've made it to the hidden settings!" +
					" Sometimes settings show up here that aren't meant for normal usage.")
			}
		}
    }
}
