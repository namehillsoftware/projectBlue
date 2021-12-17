package com.lasthopesoftware.bluewater.shared.android.services

class BindingUnexpectedlyDiedException(clazz: Class<*>) : Exception("Binding for ${clazz.canonicalName} unexpectedly died.")
