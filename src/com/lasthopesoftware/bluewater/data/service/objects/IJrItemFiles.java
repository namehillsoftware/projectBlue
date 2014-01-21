package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.util.ArrayList;

public interface IJrItemFiles {
	ArrayList<JrFile> getFiles();
	ArrayList<JrFile> getFiles(int option);
	String getFileStringList() throws IOException;
	String getFileStringList(int option) throws IOException;
}
