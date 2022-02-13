package com.lasthopesoftware.bluewater.shared.android.viewmodels

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider

@MainThread
inline fun <reified V: ViewModel> ComponentActivity.buildViewModelLazily(noinline initializer: () -> V) =
	ViewModelLazy(
		viewModelClass = V::class,
		storeProducer = { viewModelStore },
		factoryProducer = {
			return@ViewModelLazy object : ViewModelProvider.Factory {
				@Suppress("UNCHECKED_CAST")
				override fun <T : ViewModel> create(modelClass: Class<T>): T = initializer.invoke() as T
			}
		}
	)

@MainThread
inline fun <reified V: ViewModel> Fragment.buildViewModelLazily(noinline initializer: () -> V) =
	ViewModelLazy(
		viewModelClass = V::class,
		storeProducer = { viewModelStore },
		factoryProducer = {
			return@ViewModelLazy object : ViewModelProvider.Factory {
				@Suppress("UNCHECKED_CAST")
				override fun <T : ViewModel> create(modelClass: Class<T>): T = initializer.invoke() as T
			}
		}
	)

inline fun <reified V: ViewModel> Fragment.buildActivityViewModelLazily(noinline initializer: () -> V) =
	ViewModelLazy(
		viewModelClass = V::class,
		storeProducer = { requireActivity().viewModelStore },
		factoryProducer = {
			return@ViewModelLazy object : ViewModelProvider.Factory {
				@Suppress("UNCHECKED_CAST")
				override fun <T : ViewModel> create(modelClass: Class<T>): T = initializer.invoke() as T
			}
		}
	)

@MainThread
inline fun <reified V: ViewModel> ComponentActivity.buildViewModel(noinline initializer: () -> V): V =
	ViewModelProvider(viewModelStore, object : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T = initializer.invoke() as T
	})[V::class.java]
