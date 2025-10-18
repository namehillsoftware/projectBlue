package com.lasthopesoftware.bluewater.client.connection.okhttp

import okhttp3.OkHttpClient

interface ProvideOkHttpServerClients<TConnectionDetails> {
    fun getStreamingOkHttpClient(connectionDetails: TConnectionDetails): OkHttpClient
}
