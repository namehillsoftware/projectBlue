package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface

@Composable
fun ConnectionLostView(
	onCancel: () -> Unit,
	onRetry: () -> Unit,
) {
	ControlSurface {
		Box(modifier = Modifier.fillMaxSize()) {
			Column(modifier = Modifier.align(Alignment.Center)) {
				ProvideTextStyle(MaterialTheme.typography.h5) {
					Text(
						text = stringResource(id = R.string.connection_lost_retry_prompt),
						modifier = Modifier.align(Alignment.CenterHorizontally),
						textAlign = TextAlign.Center
					)
				}

				Row(
					modifier = Modifier
						.align(Alignment.CenterHorizontally)
						.padding(top = 28.dp),
					horizontalArrangement = Arrangement.SpaceEvenly,
				) {
					val buttonPadding = 16.dp
					Button(
						modifier = Modifier.padding(buttonPadding),
						onClick = onCancel,
					) {
						Text(text = stringResource(id = R.string.btn_cancel),)
					}

					Button(
						modifier = Modifier.padding(buttonPadding),
						onClick = onRetry,
					) {
						Text(text = stringResource(id = R.string.retry),)
					}
				}
			}
		}
	}
}
