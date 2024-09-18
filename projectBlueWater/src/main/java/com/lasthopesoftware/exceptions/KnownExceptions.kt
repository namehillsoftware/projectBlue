package com.lasthopesoftware.exceptions

import java.io.IOException
import java.util.Locale

private val socketClosedMessages by lazy { arrayOf("socket closed", "socket is closed") }

fun IOException.isOkHttpCanceled(): Boolean = message?.lowercase(Locale.getDefault()) == "canceled"
fun IOException.isSocketClosedException(): Boolean = socketClosedMessages.contains(message?.lowercase(Locale.getDefault()))
