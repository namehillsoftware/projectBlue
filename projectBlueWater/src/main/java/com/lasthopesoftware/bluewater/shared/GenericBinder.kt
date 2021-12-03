package com.lasthopesoftware.bluewater.shared

import android.app.Service
import android.os.Binder

/**
 * Created by david on 8/19/15.
 */
class GenericBinder<TService : Service>(val service: TService) : Binder()
