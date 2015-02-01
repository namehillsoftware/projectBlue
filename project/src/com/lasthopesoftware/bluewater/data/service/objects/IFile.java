package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;

public interface IFile extends IIntKeyStringValue {

	IFile getNextFile();
	void setNextFile(final IFile file);
	IFile getPreviousFile();
	void setPreviousFile(final IFile file);
	
	void setProperty(String name, String value);
	String getProperty(String name) throws IOException;
	String getRefreshedProperty(String name) throws IOException;
	int getDuration() throws IOException;
}
