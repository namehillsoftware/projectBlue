package com.lasthopesoftware.bluewater.android.services

class BindingUnexpectedlyDiedException(clazz: Class<*>) : Exception("Binding for ${clazz.canonicalName} unexpectedly died.")
