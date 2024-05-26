package com.lasthopesoftware.bluewater.shared.android.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.shared.android.ui.theme.LocalControlColor
import com.lasthopesoftware.promises.PromiseDelay
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.launch
import org.joda.time.Duration

private class TimeoutLatch(private val timeout: Duration) {

	private val sync = Any()

	@Volatile
	private var promisedTimeout = false.toPromise()

	@Volatile
	private var isTimedOutState = false

	val isActivated
		get() = isTimedOutState

	fun wait(): Promise<Boolean> = synchronized(sync) {
		promisedTimeout.cancel()
		promisedTimeout = Promise.Proxy {
			PromiseDelay
				.delay<Any?>(timeout)
				.also(it::doCancel)
				.then { _, cp -> isTimedOutState = !cp.isCancelled; !cp.isCancelled }
		}
		return promisedTimeout
	}

	fun cancel() = synchronized(sync) {
		promisedTimeout.cancel()
	}
}

// Courtesy of https://github.com/thesauri/dpad-compose/blob/main/app/src/main/java/dev/berggren/DpadFocusable.kt
@OptIn(ExperimentalFoundationApi::class)
@ExperimentalComposeUiApi
fun Modifier.navigable(
	onClick: () -> Unit,
	onClickLabel: String? = null,
	borderWidth: Dp = 1.dp,
	unfocusedBorderColor: Color? = null,
	focusedBorderColor: Color? = null,
	indication: Indication? = null,
	interactionSource: MutableInteractionSource? = null,
	scrollPadding: Rect = Rect.Zero,
	isDefault: Boolean = false,
	enabled: Boolean = true,
	onLongClick: (() -> Unit)? = null,
	onLongClickLabel: String? = null,
	onNavigatedTo: (() -> Unit)? = null
) = composed {
	val focusRequester = remember { FocusRequester() }
	val boxInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

	val inputMode = LocalInputModeManager.current

	LaunchedEffect(inputMode.inputMode) {
		when (inputMode.inputMode) {
			InputMode.Keyboard -> {
				if (isDefault) {
					focusRequester.requestFocus()
				}
			}
			InputMode.Touch -> {}
		}
	}

	if (inputMode.inputMode == InputMode.Touch) {
		this.combinedClickable(
			interactionSource = boxInteractionSource,
			indication = indication,
			onLongClick = onLongClick,
			onLongClickLabel = onLongClickLabel,
			onClickLabel = onClickLabel,
			onClick = onClick,
			enabled = enabled,
		)
	} else {
		val viewConfiguration = LocalViewConfiguration.current
		val longPressState = remember {
			TimeoutLatch(Duration.millis(viewConfiguration.longPressTimeoutMillis))
		}

		val isItemFocused by boxInteractionSource.collectIsFocusedAsState()
		var previousFocus: FocusInteraction.Focus? by remember {
			mutableStateOf(null)
		}
		var previousPress: PressInteraction.Press? by remember {
			mutableStateOf(null)
		}
		val scope = rememberCoroutineScope()

		LaunchedEffect(isItemFocused) {
			previousPress?.let {
				if (!isItemFocused) {
					boxInteractionSource.emit(
						PressInteraction.Release(
							press = it
						)
					)
				}
			}
		}

		val bringIntoViewRequester = remember { BringIntoViewRequester() }

		var boxSize by remember {
			mutableStateOf(IntSize(0, 0))
		}

		val animatedBorderColor by animateColorAsState(
			targetValue =
				if (isItemFocused) focusedBorderColor ?: LocalControlColor.current
				else unfocusedBorderColor ?: Color.Transparent, label = ""
		)

		this
			.bringIntoViewRequester(bringIntoViewRequester)
			.onSizeChanged {
				boxSize = it
			}
			.indication(
				interactionSource = boxInteractionSource,
				indication = indication,
			)
			.onFocusChanged { focusState ->
				if (focusState.isFocused) {
					val newFocusInteraction = FocusInteraction.Focus()
					scope.launch {
						boxInteractionSource.emit(newFocusInteraction)
					}
					scope.launch {
						val visibilityBounds = Rect(
							left = -1f * scrollPadding.left,
							top = -1f * scrollPadding.top,
							right = boxSize.width + scrollPadding.right,
							bottom = boxSize.height + scrollPadding.bottom
						)
						bringIntoViewRequester.bringIntoView(visibilityBounds)
					}
					previousFocus = newFocusInteraction
					onNavigatedTo?.invoke()
				} else {
					previousFocus?.let {
						scope.launch {
							boxInteractionSource.emit(FocusInteraction.Unfocus(it))
						}
					}
				}
			}
			.onInterceptKeyBeforeSoftKeyboard {
				if (!listOf(Key.DirectionCenter, Key.Enter).contains(it.key)) {
					return@onInterceptKeyBeforeSoftKeyboard false
				}

				longPressState.cancel()

				when (it.type) {
					KeyEventType.KeyDown -> {

						val press =
							PressInteraction.Press(
								pressPosition = Offset(
									x = boxSize.width / 2f,
									y = boxSize.height / 2f
								)
							)
						scope.launch {
							boxInteractionSource.emit(press)
						}

						if (onLongClick != null) {
							scope.launch {
								val isTimedOut = longPressState.wait().suspend()

								if (isTimedOut)
									onLongClick()
							}
						}
						previousPress = press
						true
					}

					KeyEventType.KeyUp -> {
						previousPress?.let { previousPress ->
							if (enabled && !longPressState.isActivated)
								onClick()

							scope.launch {
								boxInteractionSource.emit(
									PressInteraction.Release(
										press = previousPress
									)
								)
							}
						}
						true
					}

					else -> {
						false
					}
				}
			}
			.focusRequester(focusRequester)
			.focusTarget()
			.border(
				width = borderWidth,
				color = animatedBorderColor
			)
	}
}
