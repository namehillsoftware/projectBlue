package com.lasthopesoftware.bluewater.client.connection.selected

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.R

@Composable
fun ConnectionUpdatesView(
	connectionViewModel: InstantiateSelectedConnectionViewModel
) {
	Box(modifier = Modifier.fillMaxSize()) {
		Column(modifier = Modifier.align(Alignment.Center)) {
			ProvideTextStyle(MaterialTheme.typography.h5) {
				val connectionText by connectionViewModel.connectionStatus.collectAsState()
				Text(text = connectionText, modifier = Modifier.align(Alignment.CenterHorizontally))
			}

			CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))

			Button(
				onClick = connectionViewModel::cancelCurrentCheck,
				modifier = Modifier.align(Alignment.CenterHorizontally)
			) {
				Text(text = stringResource(id = R.string.btn_cancel),)
			}
		}
	}
}
