package com.lasthopesoftware.bluewater.shared.android.services

import android.content.ServiceConnection

class ConnectedServiceBinding<TService>(val service: TService?, val serviceConnection: ServiceConnection)
