package com.lasthopesoftware.bluewater.shared.android.viewmodels

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

@MainThread
inline fun <reified V: ViewModel> ViewModelStoreOwner.buildViewModelLazily(noinline initializer: () -> V) =
	ViewModelLazy(
		viewModelClass = V::class,
		storeProducer = { viewModelStore },
		factoryProducer = PassThroughFactory(initializer)
	)

class PassThroughFactory<V>(private val initializer: () -> V) : ViewModelProvider.Factory, () -> ViewModelProvider.Factory {
	@Suppress("UNCHECKED_CAST")
	override fun <T : ViewModel> create(modelClass: Class<T>): T = initializer() as T

	override fun invoke(): ViewModelProvider.Factory = this
}
