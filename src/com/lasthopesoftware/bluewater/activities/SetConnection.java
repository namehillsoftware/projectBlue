package com.lasthopesoftware.bluewater.activities;

import java.util.HashSet;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.objects.JrFileSystem;
import com.lasthopesoftware.bluewater.data.objects.JrSession;
import com.lasthopesoftware.threading.ISimpleTask;

public class SetConnection extends FragmentActivity {
	private Button mConnectionButton;
	private HashSet<Integer> mSelectedViews = new HashSet<Integer>();
	private SparseArray<Integer> mViews;

	private OnClickListener mConnectionButtonListener = new OnClickListener() {
        public void onClick(View v) {
        	EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
        	EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
        	EditText txtPassword = (EditText)findViewById(R.id.txtPassword);

        	JrSession.AccessCode = txtAccessCode.getText().toString();
        	JrSession.UserAuthCode = Base64.encodeToString((txtUserName.getText().toString() + ":" + txtPassword.getText().toString()).getBytes(), Base64.DEFAULT).trim();
        	
        	JrSession.IsLocalOnly = ((CheckBox)findViewById(R.id.chkLocalOnly)).isChecked();
        	
        	JrSession.SaveSession(v.getContext());
        	
        	if (!JrSession.CreateSession(getSharedPreferences(JrSession.PREFS_FILE, 0))) return;
        	
        	mConnectionButton.setText(R.string.btn_connecting);
        	mConnectionButton.setEnabled(false);
        	
        	if (JrSession.JrFs == null) JrSession.JrFs = new JrFileSystem();
        	
        	JrSession.JrFs.setOnItemsCompleteListener(new OnCompleteListener<List<IJrItem<?>>>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, List<IJrItem<?>>> owner, List<IJrItem<?>> result) {					
					String[] views = new String[result.size()];
					
					for (int i = 0; i < result.size(); i++) {
						views[i] = result.get(i).getValue();
						mViews.put(i, result.get(i).getKey());
					}
					
					showViewsSelectionDialog(views);
				}
			});
        }
    };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         
        setContentView(R.layout.activity_set_connection);
        
        mConnectionButton = (Button)findViewById(R.id.btnConnect);
        mConnectionButton.setOnClickListener(mConnectionButtonListener);
        
        if (!JrSession.CreateSession(getSharedPreferences(JrSession.PREFS_FILE, 0))) return;
        
    	EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
    	EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
    	EditText txtPassword = (EditText)findViewById(R.id.txtPassword);
    	
    	((CheckBox)findViewById(R.id.chkLocalOnly)).setChecked(JrSession.IsLocalOnly);
    	
    	txtAccessCode.setText(JrSession.AccessCode);
    	String decryptedUserAuth = new String(Base64.decode(JrSession.UserAuthCode, Base64.DEFAULT));
    	if (!decryptedUserAuth.isEmpty()) {
	    	String[] userDetails = decryptedUserAuth.split(":",2);
	    	txtUserName.setText(userDetails[0]);
	    	txtPassword.setText(userDetails[1] != null ? userDetails[1] : "");
    	}
	}
	
	private AlertDialog showViewsSelectionDialog(String[] views) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setTitle(R.string.title_activity_select_library);
		builder.setMultiChoiceItems(views, null, new DialogInterface.OnMultiChoiceClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				if (isChecked) mSelectedViews.add(mViews.get(which));
				else mSelectedViews.remove(mViews.get(which));
			}
		});
		
		final SetConnection setConnection = this;
		builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int[] selectedViews = new int[mSelectedViews.size()];
				int i = 0;
				for (int key : mSelectedViews)
					selectedViews[i++] = key;
				
				JrSession.setLibraryKeys(selectedViews);
				
				JrSession.SaveSession(setConnection);
				
				JrSession.JrFs = new JrFileSystem(selectedViews);
				Intent intent = new Intent(setConnection, BrowseLibrary.class);
				startActivity(intent);
			}
		});
		
		builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mConnectionButton.setText(R.string.btn_connect);
				mConnectionButton.setEnabled(true);
			}
		});
		
		return builder.show();
	}
}
