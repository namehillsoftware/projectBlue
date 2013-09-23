package com.lasthopesoftware.bluewater.activities;

import java.util.HashMap;
import java.util.List;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.access.JrSession;
import com.lasthopesoftware.bluewater.data.objects.JrItem;
import com.lasthopesoftware.threading.ISimpleTask;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

public class SelectLibrary extends FragmentActivity {
	private RelativeLayout mRlSelectLibraries;
	private RadioGroup mRgLibraries;
	private ProgressBar mPb;
	private Button mBtnBrowseLibraries;
	private HashMap<RadioButton, Integer> libraries;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	if (!JrSession.Active && !JrSession.CreateSession(getSharedPreferences(JrSession.PREFS_FILE, 0))) {
    		Intent intent = new Intent(this, SetConnection.class);
    		startActivity(intent);
    		return;
    	}
        
        setContentView(R.layout.activity_select_library);
        
        mRlSelectLibraries = (RelativeLayout) findViewById(R.id.rlLibrarySelectionSubLayout);
        mRgLibraries = (RadioGroup) findViewById(R.id.rgLibrarySelection);
        mPb = (ProgressBar) findViewById(R.id.pbLibrarySelection);
        mBtnBrowseLibraries = (Button) findViewById(R.id.btnBrowseLibrary);
        
        mBtnBrowseLibraries.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(), BrowseLibrary.class);
				startActivity(intent);
			}
		});
        
        JrSession.JrFs.setOnItemsCompleteListener(new OnCompleteListener<List<JrItem>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, List<JrItem>> owner, List<JrItem> result) {
				libraries = new HashMap<RadioButton, Integer>(result.size());
				for (JrItem library : result) {
					RadioButton rb = new RadioButton(mRgLibraries.getContext());
					rb.setText(library.getValue());
					rb.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							JrSession.LibraryKey = libraries.get((RadioButton)v);
						}
					});
					libraries.put(rb, library.getKey());
					mRgLibraries.addView(rb);
				}
				
				mPb.setVisibility(View.INVISIBLE);
				mRlSelectLibraries.setVisibility(View.VISIBLE);
			}
		});
	}
}
