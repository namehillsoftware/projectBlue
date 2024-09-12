package com.lasthopesoftware.exceptions

import java.io.IOException
import java.net.SocketException
import java.util.Locale

private val socketClosedMessages by lazy { arrayOf("socket closed", "socket is closed") }

fun SocketException.isSocketClosedException() = socketClosedMessages.contains(message?.lowercase(Locale.getDefault()))
fun IOException.isOkHttpCanceled(): Boolean = message?.lowercase(Locale.getDefault()) == "canceled"
