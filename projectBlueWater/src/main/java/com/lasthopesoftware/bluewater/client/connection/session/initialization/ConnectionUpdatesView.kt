package com.lasthopesoftware.bluewater.client.connection.session.initialization

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface

@Composable
fun ConnectionUpdatesView(
	connectionViewModel: ConnectionStatusViewModel
) {
	ControlSurface {
		Box(modifier = Modifier.fillMaxSize()) {
			Column(modifier = Modifier.align(Alignment.Center)) {
				ProvideTextStyle(MaterialTheme.typography.h5) {
					val connectionText by connectionViewModel.connectionStatus.collectAsState()
					Text(text = connectionText, modifier = Modifier.align(Alignment.CenterHorizontally))
				}

				CircularProgressIndicator(
					modifier = Modifier
						.align(Alignment.CenterHorizontally)
						.padding(top = 28.dp)
				)

				Button(
					onClick = connectionViewModel::cancelCurrentCheck,
					modifier = Modifier
						.align(Alignment.CenterHorizontally)
						.padding(top = 8.dp)
				) {
					Text(text = stringResource(id = R.string.btn_cancel),)
				}
			}
		}
	}
}
