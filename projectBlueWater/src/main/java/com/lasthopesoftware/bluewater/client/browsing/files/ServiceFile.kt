package com.lasthopesoftware.bluewater.client.browsing.files

import android.os.Parcelable
import com.lasthopesoftware.bluewater.shared.Key
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServiceFile(override val key: String = "") : Key, Parcelable
