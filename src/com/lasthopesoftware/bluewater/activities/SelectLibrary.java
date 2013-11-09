package com.lasthopesoftware.bluewater.activities;

import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.objects.JrFileSystem;
import com.lasthopesoftware.bluewater.data.objects.JrSession;
import com.lasthopesoftware.threading.ISimpleTask;

public class SelectLibrary extends FragmentActivity {
	private LinearLayout mRlSelectLibraries;
	private RadioGroup mRgLibraries;
	private ProgressBar mPb;
	private Button mBtnBrowseLibraries;
	private HashMap<RadioButton, Integer> libraries;
	private int mSelectedLibrary = -1;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	if (!JrSession.Active && !JrSession.CreateSession(getSharedPreferences(JrSession.PREFS_FILE, 0))) {
    		Intent intent = new Intent(this, SetConnection.class);
    		startActivity(intent);
    		return;
    	}
        
        setContentView(R.layout.activity_select_library);
        
        mRlSelectLibraries = (LinearLayout) findViewById(R.id.rlLibrarySelectionSubLayout);
        mRgLibraries = (RadioGroup) findViewById(R.id.rgLibrarySelection);
        mPb = (ProgressBar) findViewById(R.id.pbLibrarySelection);
        mBtnBrowseLibraries = (Button) findViewById(R.id.btnBrowseLibrary);
        
        mBtnBrowseLibraries.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mSelectedLibrary < 0) return;
				
//				JrSession.LibraryKey = mSelectedLibrary;
				JrSession.JrFs = new JrFileSystem(JrSession.getLibraryKeys());
				JrSession.SaveSession(v.getContext());
				Intent intent = new Intent(v.getContext(), BrowseLibrary.class);
				startActivity(intent);
			}
		});
        
        if (JrSession.JrFs == null) JrSession.JrFs = new JrFileSystem();
        
        JrSession.JrFs.setOnItemsCompleteListener(new OnCompleteListener<List<IJrItem<?>>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, List<IJrItem<?>>> owner, List<IJrItem<?>> result) {
				libraries = new HashMap<RadioButton, Integer>(result.size());
				for (IJrItem<?> library : result) {
					RadioButton rb = new RadioButton(mRgLibraries.getContext());
					rb.setText(library.getValue());
//					rb.setChecked(JrSession.LibraryKey == library.getKey());
					rb.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							mSelectedLibrary = libraries.get((RadioButton)v);
						}
					});
					libraries.put(rb, library.getKey());
					mRgLibraries.addView(rb);
				}
				
				mPb.setVisibility(View.INVISIBLE);
				mRlSelectLibraries.setVisibility(View.VISIBLE);
			}
		});
        
        JrSession.JrFs.getSubItemsAsync();
	}
}
