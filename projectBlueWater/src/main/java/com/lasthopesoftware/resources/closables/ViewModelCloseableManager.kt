package com.lasthopesoftware.resources.closables

import androidx.lifecycle.ViewModel

class ViewModelCloseableManager : ViewModel(), ManageCloseables {
	private val inner = AutoCloseableManager()

	override fun manage(closeable: AutoCloseable) {
		inner.manage(closeable)
	}

	override fun onCleared() {
		inner.close()
	}
}
