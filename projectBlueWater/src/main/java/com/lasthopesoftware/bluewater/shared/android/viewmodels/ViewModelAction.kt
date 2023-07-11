package com.lasthopesoftware.bluewater.shared.android.viewmodels

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ViewModelInitAction(action: @Composable () -> Unit) = viewModel {
	ViewModelActionRunner(action)
}.run { Run() }

private class ViewModelActionRunner(private val action: @Composable () -> Unit) : ViewModel() {
	private val didActionRun = AtomicBoolean(false)

	@Composable
	fun Run() {
		if (didActionRun.compareAndSet(false, true)) action()
	}
}
