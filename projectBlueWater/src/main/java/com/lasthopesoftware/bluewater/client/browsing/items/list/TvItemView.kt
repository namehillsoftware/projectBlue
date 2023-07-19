package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
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
	tvChildItemViewModelProvider: PooledCloseablesViewModel<ReusableTvChildItemViewModel>,
    trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
) {
    Column {
        val itemTitle by itemListViewModel.itemValue.collectAsState()

        Text(
            text = itemTitle,
            style = androidx.tv.material3.MaterialTheme.typography.headlineMedium,
        )

        val childItems by itemListViewModel.items.collectAsState()
        Text(
            text = stringResource(id = R.string.item_count_label, childItems.size),
            style = androidx.tv.material3.MaterialTheme.typography.headlineSmall,
        )

        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.viewPaddingUnit * 2)
        ) {
            items(childItems) { child ->
                Card(
					modifier = Modifier.width(300.dp),
                    onClick = {
                        itemListViewModel.loadedLibraryId?.also {
                            navigateApplication.viewItem(it, child)
                        }
                    }
                ) {
					Column(
						modifier = Modifier.fillMaxWidth()
					) {
						Box(
							modifier = Modifier.height(300.dp).fillMaxWidth()
						) {
							val childItemViewModel = remember(tvChildItemViewModelProvider::getViewModel)

							DisposableEffect(key1 = child) {
								itemListViewModel.loadedLibraryId?.also {
									childItemViewModel.update(it, child)
								}

								onDispose {
									childItemViewModel.reset()
								}
							}

							val itemBitmap by childItemViewModel.itemImage.collectAsState()
							val itemImageBitmap by remember { derivedStateOf { itemBitmap?.asImageBitmap() } }
							itemImageBitmap?.also {
								Image(
									bitmap = it,
									contentDescription = child.value ?: "",
									contentScale = ContentScale.FillHeight,
									modifier = Modifier
										.clip(RoundedCornerShape(5.dp))
										.border(
											1.dp,
											shape = RoundedCornerShape(5.dp),
											color = MaterialTheme.colors.onSurface,
										)
										.fillMaxHeight()
										.align(Alignment.Center),
								)
							}
						}

						Text(text = child.value ?: "")
					}
                }
            }
        }

        val childFiles by fileListViewModel.files.collectAsState()
        Text(
            text = stringResource(id = R.string.file_count_label, childFiles.size),
            style = androidx.tv.material3.MaterialTheme.typography.headlineSmall,
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
