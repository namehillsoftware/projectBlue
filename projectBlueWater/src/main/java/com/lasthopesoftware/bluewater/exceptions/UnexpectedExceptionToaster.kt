package com.lasthopesoftware.bluewater.exceptions

import android.content.Context
import android.widget.Toast

class UnexpectedExceptionToaster(private val context: Context) : AnnounceExceptions {
    override fun announce(error: Throwable) {
        Toast.makeText(
            context,
            "An unexpected error occurred! The error was " + error.javaClass.name,
            Toast.LENGTH_SHORT
        ).show()
    }
}
