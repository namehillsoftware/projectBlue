import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.lasthopesoftware.bluewater.android.ui.remember

@Composable
fun VerticalHeaderScaffold(
	header: @Composable () -> Unit,
	overlay: @Composable () -> Unit,
	content: @Composable (Dp) -> Unit,
	modifier: Modifier = Modifier,
) {
	Box(modifier = modifier) {
		var headerHeight by rememberSaveable { mutableIntStateOf(0) }

		val actualContent = LocalDensity.current.remember(content, headerHeight) {
			val headerHeightDp = headerHeight.toDp()
			Log.d("VerticalHeaderScaffold", "maxOverlayHeightDp = $headerHeightDp")

			return@remember @Composable {
				content(headerHeightDp)
			}
		}

		Layout(
			contents = listOf(
				{
					Layout(contents = listOf(header, overlay)) { (headerMeasurable, overhangMeasurable), constraints ->
						val headerPlaceables = headerMeasurable.map { it.measure(constraints) }
						val overlayPlaceables = overhangMeasurable.map { it.measure(constraints) }

						layout(constraints.minWidth, constraints.minHeight) {
							var currentY = 0

							for (placeable in headerPlaceables) {
								placeable.place(x = 0, y = currentY)
								currentY += placeable.height
							}

							for (placeable in overlayPlaceables) {
								placeable.place(x = 0, y = currentY)
								currentY += placeable.height
							}
							headerHeight = maxOf(currentY, headerHeight)
						}
					}
				},
				{
					Layout(content = actualContent) { measurables, constraints ->
						val placeables = measurables.map { it.measure(constraints) }

						layout(constraints.maxWidth, constraints.maxHeight) {
							var currentY = 0

							for (placeable in placeables) {
								placeable.place(x = 0, y = currentY)
								currentY += placeable.height
							}
						}
					}
				}
			)
		) { (headerWithOverlayMeasurable, contentMeasurable), constraints ->
			val headerPlaceables = headerWithOverlayMeasurable.map { it.measure(constraints) }

			val contentPlaceables = contentMeasurable.map { it.measure(constraints) }

			layout(constraints.maxWidth, constraints.maxHeight) {
				var currentY = 0
				for (placeable in contentPlaceables) {
					placeable.place(x = 0, y = currentY)
					currentY += placeable.height
				}

				currentY = 0
				for (placeable in headerPlaceables) {
					placeable.place(x = 0, y = currentY)
					currentY += placeable.height
				}
			}
		}
	}
}
