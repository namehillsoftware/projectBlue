package com.lasthopesoftware.bluewater.client.playback.service

class BindingUnexpectedlyDiedException(clazz: Class<*>) : Exception("Binding for ${clazz.canonicalName} unexpectedly died.")
