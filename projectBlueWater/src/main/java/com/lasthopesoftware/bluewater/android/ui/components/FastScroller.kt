package com.lasthopesoftware.bluewater.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// Source: https://medium.com/@kezzieleo/implementing-a-custom-fast-scroller-in-jetpack-compose-502f2d69c124
@Composable
fun FastScroller(
	listState: LazyListState, // State of the LazyColumn to track scrolling
	modifier: Modifier = Modifier,
	floatingLetterLabel: String = "", // Label that shows the first letter of the visible item
	scrollBarHeight : MutableState<Int> = remember { mutableIntStateOf(1) }, // Holds the height of the scroll bar
	thumbHeight : Float = with(LocalDensity.current) { 72.dp.toPx() }, // Height of the scroll thumb
) {
	val scope = rememberCoroutineScope()
	var thumbOffsetY by remember { mutableFloatStateOf(0f) } // Current vertical position of the scroll thumb
	var isDragging by remember { mutableStateOf(false) } // Tracks if the user is dragging the thumb
	var isScrolling by remember { mutableStateOf(false) }
	LaunchedEffect(listState) {
		snapshotFlow { listState.isScrollInProgress }
			.collect {
				if (it) isScrolling = true
				else {
					delay(1500)
					isScrolling = false
				}
			}
	}

	LaunchedEffect(listState) {
		snapshotFlow { listState.layoutInfo }
			.collect { info ->
				val firstVisibleListItem = listState.firstVisibleItemIndex.toFloat()
				val totalScrollableHeight = scrollBarHeight.value - thumbHeight
				// Calculate the new thumb position based on the scroll progress
				thumbOffsetY = (firstVisibleListItem / (info.totalItemsCount - 1)) * totalScrollableHeight
			}
	}

	// Container for the fast scroller
	Row(
		modifier = modifier
			.fillMaxHeight()
			.padding(end = Dimensions.rowPadding)
			.onGloballyPositioned { layoutCoordinates ->
				// Capture the height of the scroll bar when it is positioned on the screen
				scrollBarHeight.value = layoutCoordinates.size.height
			},
		horizontalArrangement = Arrangement.End // Aligns items to the right
	) {
		// Show floating letter indicator when scrolling or dragging
		if (isScrolling || isDragging) {
			if (floatingLetterLabel.isNotEmpty()) {
				Column {
					Spacer(modifier = Modifier.height(20.dp)) // Adds spacing before the label
					Box(
						modifier = Modifier
							.offset(y = with(LocalDensity.current) { thumbOffsetY.toDp() }) // Position dynamically based on scroll
							.background(
								Color.Black.copy(alpha = 0.7f), // Semi-transparent background
								RoundedCornerShape(
									topStart = 24.dp,
									bottomStart = 24.dp,
									bottomEnd = 12.dp
								)
							)
							.padding(horizontal = 12.dp, vertical = 8.dp) // Padding inside the label box
					) {
						Text(
							text = floatingLetterLabel, // Display the current letter
							color = Color.White,
							fontSize = 24.sp,
							fontWeight = FontWeight.Bold
						)
					}
				}
			}
		}

		Spacer(modifier = Modifier.width(8.dp)) // Adds spacing between the label and the scrollbar

		// The vertical scrollbar container
		Box(
			modifier = Modifier
				.fillMaxHeight()
				.width(18.dp) // Fixed width for the scrollbar
				.pointerInput(Unit) {
					detectVerticalDragGestures(
						onDragStart = { offset ->
							isDragging = true // Set dragging flag to true
							thumbOffsetY = offset.y // Update the thumb's position
						},
						onVerticalDrag = { _, dragAmount ->
							// Calculate new thumb position while keeping it within bounds
							val newOffset = (thumbOffsetY + dragAmount)
								.coerceIn(0f, scrollBarHeight.value - thumbHeight)

							thumbOffsetY = newOffset // Update the thumb position

							val listSize = listState.layoutInfo.totalItemsCount
							// Compute the target item index based on the thumb position
							val targetIndex = ((newOffset / (scrollBarHeight.value - thumbHeight)) * (listSize - 1))
								.toInt()
								.coerceIn(0, listSize - 1)

							// Scroll to the calculated item index
							scope.launch {
								listState.scrollToItem(targetIndex)
							}
						},
						onDragEnd = {
							// Hide the scrollbar after a delay once dragging ends
							scope.launch {
								delay(1500)
								isDragging = false
							}
						}
					)
				}
		) {
			// Show the thumb indicator when scrolling or dragging
			if (isScrolling || isDragging) {
				Row(
					horizontalArrangement = Arrangement.Center,
					modifier = Modifier.fillMaxWidth()
				) {
					Box(
						modifier = Modifier
							.offset(y = with(LocalDensity.current) { thumbOffsetY.toDp() }) // Update thumb position dynamically
							.size(14.dp, 72.dp) // Set size of the scrollbar thumb
							.clip(CircleShape) // Rounded shape
							.background(Color.Gray) // Thumb color
					)
				}
			}
		}
	}
}
