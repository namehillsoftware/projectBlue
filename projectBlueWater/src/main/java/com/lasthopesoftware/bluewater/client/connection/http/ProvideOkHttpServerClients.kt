package com.lasthopesoftware.bluewater.client.connection.http

import okhttp3.OkHttpClient

interface ProvideOkHttpServerClients<TConnectionDetails> {
    fun getStreamingOkHttpClient(connectionDetails: TConnectionDetails): OkHttpClient
}
