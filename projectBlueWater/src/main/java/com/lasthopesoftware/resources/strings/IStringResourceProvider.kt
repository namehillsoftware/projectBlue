package com.lasthopesoftware.resources.strings

import androidx.annotation.StringRes

/**
 * Created by david on 7/3/16.
 */
interface IStringResourceProvider {
    fun getString(@StringRes stringResourceId: Int): String
}
