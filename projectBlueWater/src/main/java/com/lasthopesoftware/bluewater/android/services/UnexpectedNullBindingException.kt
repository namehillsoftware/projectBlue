package com.lasthopesoftware.bluewater.android.services

class UnexpectedNullBindingException(clazz: Class<*>)
	: Exception("Unexpected null binding for ${clazz.canonicalName}.")
