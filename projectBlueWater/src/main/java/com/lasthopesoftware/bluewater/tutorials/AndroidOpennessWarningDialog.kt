package com.lasthopesoftware.bluewater.tutorials

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit
import com.lasthopesoftware.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.suspend
import com.mikepenz.markdown.m2.Markdown
import kotlinx.coroutines.launch

@Composable
fun AndroidOpennessWarningDialog(androidOpennessWarningViewModel: AndroidOpennessWarningViewModel) {
	val showSideLoadingDialog by androidOpennessWarningViewModel.isShown.subscribeAsState()

	LaunchedEffect(androidOpennessWarningViewModel) {
		androidOpennessWarningViewModel
			.promiseLoadedSideLoadingTutorial()
			.suspend()
	}

	if (showSideLoadingDialog) {
		val scope = rememberCoroutineScope()
		Dialog(
			onDismissRequest = {
				scope.launch { androidOpennessWarningViewModel.promiseSideLoadingMarkedShown().suspend() }
			},
		) {
			ControlSurface {
				Column(
					modifier = Modifier.padding(viewPaddingUnit * 2),
				) {
					ProvideTextStyle(MaterialTheme.typography.h5) {
						Text(
							text = stringResource(R.string.keep_android_open),
							modifier = Modifier
								.align(Alignment.CenterHorizontally),
						)
					}

					Markdown(
						content = stringResource(R.string.side_loading_warning),
						modifier = Modifier
							.wrapContentSize()
							.padding(vertical = Dimensions.rowPadding),
					)

					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
					) {
						val context = LocalContext.current
						Button(
							onClick = {
								context
									.startActivity(Intent(
										Intent.ACTION_VIEW,
										"https://keepandroidopen.org".toUri()
									))
							},
						) {
							Text(text = stringResource(R.string.more_info))
						}
						Button(
							onClick = {
								context
									.startActivity(Intent(
										Intent.ACTION_VIEW,
										"https://github.com/woheller69/FreeDroidWarn?tab=readme-ov-file#solutions".toUri()
									))
							},
						) {
							Text(text = stringResource(R.string.solutions))
						}
					}
				}
			}
		}
	}
}
