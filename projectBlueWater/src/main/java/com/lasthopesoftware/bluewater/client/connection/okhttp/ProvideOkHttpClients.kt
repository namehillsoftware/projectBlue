package com.lasthopesoftware.bluewater.client.connection.okhttp

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import okhttp3.OkHttpClient

interface ProvideOkHttpClients {
    fun getOkHttpClient(urlProvider: IUrlProvider): OkHttpClient
}
