package com.lasthopesoftware.bluewater.shared

interface IIntKeyStringValue : IIntKey<IIntKeyStringValue> {
    val value: String?
}
