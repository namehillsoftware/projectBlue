package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import com.lasthopesoftware.threading.ISimpleTask;

import java.util.ArrayList;

public interface IItemFiles {
	ArrayList<IFile> getFiles();
	ArrayList<IFile> getFiles(int option);
	void getFileStringList(ISimpleTask.OnCompleteListener<String, Void, String> onGetStringListComplete);
	void getFileStringList(ISimpleTask.OnCompleteListener<String, Void, String> onGetStringListComplete, final ISimpleTask.OnErrorListener<String, Void, String> onGetStringListError);
	void getFileStringList(final int option, final ISimpleTask.OnCompleteListener<String, Void, String> onGetStringListComplete);
	void getFileStringList(final int option, final ISimpleTask.OnCompleteListener<String, Void, String> onGetStringListComplete, final ISimpleTask.OnErrorListener<String, Void, String> onGetStringListError);
}
