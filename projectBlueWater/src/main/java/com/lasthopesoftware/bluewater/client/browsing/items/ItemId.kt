package com.lasthopesoftware.bluewater.client.browsing.items

import kotlinx.parcelize.Parcelize

@Parcelize
@JvmInline
value class ItemId(override val id: String) : KeyedIdentifier
