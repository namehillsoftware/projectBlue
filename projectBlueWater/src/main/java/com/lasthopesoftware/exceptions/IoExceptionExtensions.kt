package com.lasthopesoftware.exceptions

import java.io.IOException
import java.util.Locale

fun IOException.isOkHttpCanceled(): Boolean = message?.lowercase(Locale.getDefault()) == "canceled"
