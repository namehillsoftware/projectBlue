package com.lasthopesoftware.bluewater.client.browsing.library.repository

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface ConnectionSourceId

@Parcelize
data class LibraryId(val id: Int) : Parcelable, ConnectionSourceId
