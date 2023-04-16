package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R

@Composable
fun ConnectionLostView(
	onCancel: () -> Unit,
	onRetry: () -> Unit,
) {
	Surface {
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
