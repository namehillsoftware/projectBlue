package com.lasthopesoftware.bluewater.client.browsing.items

import android.os.Parcelable
import com.lasthopesoftware.bluewater.shared.IIntKeyStringValue

interface IItem : IIntKeyStringValue, Parcelable {
	val itemId: KeyedIdentifier
}
