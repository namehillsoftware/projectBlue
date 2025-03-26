package com.lasthopesoftware.bluewater.client.connection.okhttp

import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import okhttp3.OkHttpClient

interface ProvideOkHttpClients {
    fun getOkHttpClient(mediaCenterConnectionDetails: MediaCenterConnectionDetails): OkHttpClient
    fun getOkHttpClient(subsonicConnectionDetails: SubsonicConnectionDetails): OkHttpClient
}
