package com.lasthopesoftware.resources.closables

import androidx.lifecycle.ViewModel

class ViewModelCloseableManager : ViewModel(), ManageCloseables {
	private val inner = AutoCloseableManager()

	override fun <T : AutoCloseable> manage(closeable: T): T {
		return inner.manage(closeable)
	}

	override fun onCleared() {
		inner.close()
	}
}
