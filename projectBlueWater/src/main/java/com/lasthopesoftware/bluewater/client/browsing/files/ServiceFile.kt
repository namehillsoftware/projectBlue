package com.lasthopesoftware.bluewater.client.browsing.files

import android.os.Parcelable
import com.lasthopesoftware.bluewater.shared.IIntKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServiceFile(override val key: Int = 0) : IIntKey<ServiceFile>, Parcelable
