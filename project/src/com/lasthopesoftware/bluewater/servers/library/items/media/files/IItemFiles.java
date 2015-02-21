package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import java.util.ArrayList;

import com.lasthopesoftware.threading.IDataTask.OnCompleteListener;
import com.lasthopesoftware.threading.IDataTask.OnErrorListener;

public interface IItemFiles {
	ArrayList<IFile> getFiles();
	ArrayList<IFile> getFiles(int option);
	void getFileStringList(OnCompleteListener<String> onGetStringListComplete);
	void getFileStringList(OnCompleteListener<String> onGetStringListComplete, final OnErrorListener<String> onGetStringListError);
	void getFileStringList(final int option, final OnCompleteListener<String> onGetStringListComplete);
	void getFileStringList(final int option, final OnCompleteListener<String> onGetStringListComplete, final OnErrorListener<String> onGetStringListError);
}
