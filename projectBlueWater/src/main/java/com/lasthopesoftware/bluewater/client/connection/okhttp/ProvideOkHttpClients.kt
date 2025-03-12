package com.lasthopesoftware.bluewater.client.connection.okhttp

import com.lasthopesoftware.bluewater.client.connection.url.ProvideUrls
import okhttp3.OkHttpClient

interface ProvideOkHttpClients {
    fun getOkHttpClient(urlProvider: ProvideUrls): OkHttpClient

    fun getJriverCentralClient(): OkHttpClient
}
