package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.util.ArrayList;

public interface IItemFiles {
	ArrayList<File> getFiles();
	ArrayList<File> getFiles(int option);
	String getFileStringList() throws IOException;
	String getFileStringList(int option) throws IOException;
}
