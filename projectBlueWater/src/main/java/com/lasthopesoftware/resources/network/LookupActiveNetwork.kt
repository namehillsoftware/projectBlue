package com.lasthopesoftware.resources.network

import android.net.NetworkInfo

interface LookupActiveNetwork {
    val activeNetworkInfo: NetworkInfo?
}
