package com.lasthopesoftware.bluewater.client.connection.okhttp

import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import okhttp3.OkHttpClient

interface ProvideOkHttpClients {
    fun getOkHttpClient(serverConnection: ServerConnection): OkHttpClient

    fun getServerDiscoveryClient(): OkHttpClient
}
