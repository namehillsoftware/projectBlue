package com.lasthopesoftware.resources.strings

import android.content.Context
import androidx.annotation.StringRes

/**
 * Created by david on 7/3/16.
 */
class StringResourceProvider(private val context: Context) : IStringResourceProvider {
    override fun getString(@StringRes stringResourceId: Int): String {
        return context.getString(stringResourceId)
    }
}
