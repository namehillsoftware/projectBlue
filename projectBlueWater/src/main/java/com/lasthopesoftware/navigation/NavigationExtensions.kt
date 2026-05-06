package com.lasthopesoftware.navigation

import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavEntry

fun <T> NavController<T>.peek(): NavEntry<T>? = backstack.entries.lastOrNull()
fun <T> NavController<T>.isNotEmpty(): Boolean = backstack.entries.isNotEmpty()
