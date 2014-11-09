package com.lasthopesoftware.bluewater.data.service.objects;

import java.util.ArrayList;

import com.lasthopesoftware.threading.ISimpleTask;

public interface IItemFiles {
	ArrayList<File> getFiles();
	ArrayList<File> getFiles(int option);
	void getFileStringList(ISimpleTask.OnCompleteListener<String, Void, String> onGetStringListComplete);
	void getFileStringList(final int option, final ISimpleTask.OnCompleteListener<String, Void, String> onGetStringListComplete);
}
