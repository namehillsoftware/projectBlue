package com.lasthopesoftware.bluewater.shared;

public interface IIntKeyStringValue extends IIntKey<IIntKeyStringValue> {
	String getValue();
	void setValue(String value);
}
