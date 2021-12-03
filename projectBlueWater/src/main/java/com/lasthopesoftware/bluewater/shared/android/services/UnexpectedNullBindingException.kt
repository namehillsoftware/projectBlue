package com.lasthopesoftware.bluewater.shared.android.services

class UnexpectedNullBindingException(clazz: Class<*>)
	: Exception("Unexpected null binding for ${clazz.canonicalName}.")
