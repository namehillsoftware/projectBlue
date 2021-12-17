package com.lasthopesoftware.bluewater.shared.android.services

class InvalidBindingException(clazz: Class<*>)
	: Exception("Binding could not cast to ${clazz.canonicalName}.")
