package com.lasthopesoftware.bluewater.shared.android.viewmodels

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore

@MainThread
inline fun <reified V: ViewModel> ComponentActivity.buildViewModelLazily(noinline initializer: () -> V) =
	viewModelStore.buildViewModelLazily(initializer)

@MainThread
inline fun <reified V: ViewModel> Fragment.buildViewModelLazily(noinline initializer: () -> V) =
	viewModelStore.buildViewModelLazily(initializer)

@MainThread
inline fun <reified V: ViewModel> Fragment.buildActivityViewModelLazily(noinline initializer: () -> V) =
	requireActivity().viewModelStore.buildViewModelLazily(initializer)

@MainThread
inline fun <reified V: ViewModel> Fragment.buildActivityViewModel(noinline initializer: () -> V) =
	viewModelStore.buildViewModel(initializer)

@MainThread
inline fun <reified V: ViewModel> ComponentActivity.buildViewModel(noinline initializer: () -> V): V =
	viewModelStore.buildViewModel(initializer)

@MainThread
inline fun <reified V: ViewModel> ViewModelStore.buildViewModel(noinline initializer: () -> V): V =
	ViewModelProvider(this, PassThroughFactory(initializer))[V::class.java]

@MainThread
inline fun <reified V: ViewModel> ViewModelStore.buildViewModelLazily(noinline initializer: () -> V) =
	ViewModelLazy(
		viewModelClass = V::class,
		storeProducer = { this },
		factoryProducer = PassThroughFactory(initializer)
	)

class PassThroughFactory<V>(private val initializer: () -> V) : ViewModelProvider.Factory, () -> ViewModelProvider.Factory {
	@Suppress("UNCHECKED_CAST")
	override fun <T : ViewModel> create(modelClass: Class<T>): T = initializer() as T

	override fun invoke(): ViewModelProvider.Factory = this
}
