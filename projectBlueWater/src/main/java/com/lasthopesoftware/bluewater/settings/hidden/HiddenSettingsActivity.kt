package com.lasthopesoftware.bluewater.settings.hidden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class HiddenSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

		setContent { HiddenSettings() }
    }
}

@Preview
@Composable
fun HiddenSettings() {
	Column {
		Row {
			Column {
				Checkbox(checked = false, onCheckedChange = {})
			}

			Column {
				Text(text = "Use custom caching implementation")
			}
		}
	}
}
