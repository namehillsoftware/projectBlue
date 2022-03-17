package com.lasthopesoftware.bluewater.shared.android.services

import android.app.Service
import android.os.Binder

class GenericBinder<TService : Service>(val service: TService) : Binder()
