package com.lasthopesoftware.bluewater.client.browsing.items.list

import com.lasthopesoftware.bluewater.client.browsing.items.image.ProvideLibraryItemImages
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel

class ReusableTvChildItemViewModelProvider(
	private val itemImages: ProvideLibraryItemImages,
) : PooledCloseablesViewModel<ReusableTvChildItemViewModel>() {
	override fun getNewCloseable(): ReusableTvChildItemViewModel = ReusableTvChildItemViewModel(itemImages)
}
