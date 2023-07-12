package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvItemView(
    itemListViewModel: ItemListViewModel,
    fileListViewModel: FileListViewModel,
    navigateApplication: NavigateApplication,
    trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
) {
    Column {
        val itemTitle by itemListViewModel.itemValue.collectAsState()

        Text(
            text = itemTitle,
            style = MaterialTheme.typography.headlineMedium,
        )

        val childItems by itemListViewModel.items.collectAsState()
        Text(
            text = stringResource(id = R.string.item_count_label, childItems.size),
            style = MaterialTheme.typography.headlineSmall,
        )

        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.viewPaddingUnit * 2)
        ) {
            items(childItems) { child ->
                Card(
                    onClick = {
                        itemListViewModel.loadedLibraryId?.also {
                            navigateApplication.viewItem(it, child)
                        }
                    }
                ) {
                    Text(text = child.value ?: "")
                }
            }
        }

        val childFiles by fileListViewModel.files.collectAsState()
        Text(
            text = stringResource(id = R.string.file_count_label, childFiles.size),
            style = MaterialTheme.typography.headlineSmall,
        )

        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.viewPaddingUnit * 2)
        ) {
            itemsIndexed(childFiles) { i, serviceFile ->
                Card(
                    onClick = {
                        itemListViewModel.loadedLibraryId?.also {
                            navigateApplication.viewFileDetails(it, childFiles, i)
                        }
                    }
                ) {
                    val fileItemViewModel = remember(trackHeadlineViewModelProvider::getViewModel)

                    DisposableEffect(serviceFile) {
                        itemListViewModel.loadedLibraryId?.also {
                            fileItemViewModel.promiseUpdate(it, serviceFile)
                        }

                        onDispose {
                            fileItemViewModel.reset()
                        }
                    }

                    val title by fileItemViewModel.title.collectAsState()

                    Text(text = title)
                }
            }
        }
    }
}
