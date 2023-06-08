package com.lasthopesoftware.bluewater.shared.exceptions

import android.content.Context
import android.widget.Toast

object UnexpectedExceptionToaster {
    fun announce(context: Context?, error: Throwable) {
        Toast.makeText(
            context,
            "An unexpected error occurred! The error was " + error.javaClass.name,
            Toast.LENGTH_SHORT
        ).show()
    }
}
