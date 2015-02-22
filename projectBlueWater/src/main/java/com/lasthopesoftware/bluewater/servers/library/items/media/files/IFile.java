package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import com.lasthopesoftware.bluewater.shared.IIntKeyStringValue;

import java.io.IOException;

public interface IFile extends IIntKeyStringValue {	
	void setProperty(String name, String value);
	String getProperty(String name) throws IOException;
	String getRefreshedProperty(String name) throws IOException;
	int getDuration() throws IOException;
}
