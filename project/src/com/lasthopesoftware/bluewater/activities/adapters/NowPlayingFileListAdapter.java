package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.List;

import com.lasthopesoftware.bluewater.data.service.objects.File;

import android.content.Context;
import android.widget.ArrayAdapter;

public class NowPlayingFileListAdapter extends ArrayAdapter<File> {

	private List<File> mFiles;
	
	public NowPlayingFileListAdapter(Context context, int resource, List<File> files) {
		super(context, resource, files);
		
		mFiles = files;
	}
}
