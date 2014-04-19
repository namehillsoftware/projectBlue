package com.lasthopesoftware.bluewater.activities;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

public class ViewTrackDetails extends Activity {

	public static final String FILE_KEY = "com.lasthopesoftware.bluewater.activities.ViewFiles.FILE_KEY";
	
	private ImageView mFileThumbnail;
	
	private JrFile mFile;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_track_details);
        
        final int fileKey = this.getIntent().getIntExtra(FILE_KEY, -1);
        
        if (fileKey < 0) return;
        
        mFileThumbnail = (ImageView) findViewById(R.id.imgFileThumbnail);
        
        mFile = new JrFile(fileKey);
	}
}
