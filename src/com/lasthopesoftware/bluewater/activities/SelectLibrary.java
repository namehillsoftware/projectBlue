package com.lasthopesoftware.bluewater.activities;

import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
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

public class SelectLibrary extends DialogFragment {
	private LinearLayout mRlSelectLibraries;
	private RadioGroup mRgLibraries;
	private ProgressBar mPb;
	private Button mBtnBrowseLibraries;
	private HashMap<RadioButton, Integer> libraries;
	private int mSelectedLibrary = -1;
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
    	if (!JrSession.Active) {
    		return null;
    	}

        // Set the dialog title
        builder.setTitle(R.string.title_activity_select_library);
        // Specify the list array, the items to be selected by default (null for none),
        // and the listener through which to receive callbacks when items are selected
        builder.setMultiChoiceItems(R.array.toppings, null,
        new DialogInterface.OnMultiChoiceClickListener() {
           @Override
           public void onClick(DialogInterface dialog, int which,
                   boolean isChecked) {
               if (isChecked) {
                   // If the user checked the item, add it to the selected items
                   mSelectedItems.add(which);
               } else if (mSelectedItems.contains(which)) {
                   // Else, if the item is already in the array, remove it 
                   mSelectedItems.remove(Integer.valueOf(which));
               }
           }
       });

        
        mBtnBrowseLibraries.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mSelectedLibrary < 0) return;
				
				JrSession.LibraryKey = mSelectedLibrary;
				JrSession.JrFs = new JrFileSystem(JrSession.LibraryKey);
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
					rb.setChecked(JrSession.LibraryKey == library.getKey());
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
