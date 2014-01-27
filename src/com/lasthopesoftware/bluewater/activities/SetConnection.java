package com.lasthopesoftware.bluewater.activities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
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
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.service.objects.JrFileSystem;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.data.sqlite.objects.SelectedView;
import com.lasthopesoftware.threading.ISimpleTask;

public class SetConnection extends FragmentActivity {
	private Button mConnectionButton;
	private HashSet<SelectedView> mSelectedViews = new HashSet<SelectedView>();
	private SparseArray<SelectedView> mViews = new SparseArray<SelectedView>();
	private Context thisContext = this;

	private OnClickListener mConnectionButtonListener = new OnClickListener() {
        public void onClick(View v) {
        	EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
        	EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
        	EditText txtPassword = (EditText)findViewById(R.id.txtPassword);
        	
        	Library library = JrSession.GetLibrary(v.getContext());

        	if (library != null) {
        		library.setAccessCode(txtAccessCode.getText().toString());
	        	library.setAuthKey(Base64.encodeToString((txtUserName.getText().toString() + ":" + txtPassword.getText().toString()).getBytes(), Base64.DEFAULT).trim());
	        	
	        	library.setLocalOnly(((CheckBox)findViewById(R.id.chkLocalOnly)).isChecked());
        	}
        	
        	JrSession.SaveSession(v.getContext());
        	
        	if (JrSession.ChooseLibrary(v.getContext(), library.getId()) == null) return;
        	
        	mConnectionButton.setText(R.string.btn_connecting);
        	mConnectionButton.setEnabled(false);
        	
        	if (JrSession.JrFs == null) JrSession.JrFs = new JrFileSystem();
        	
        	JrSession.JrFs.setOnItemsCompleteListener(new OnCompleteListener<List<IJrItem<?>>>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, List<IJrItem<?>>> owner, List<IJrItem<?>> result) {
					if (result == null || result.size() == 0) return;
					
					String[] views = new String[result.size()];
					mViews = new SparseArray<SelectedView>(result.size());
					for (int i = 0; i < result.size(); i++) {
						views[i] = result.get(i).getValue();
						SelectedView newView = new SelectedView();
						newView.setServiceKey(result.get(i).getKey());
						newView.setName(result.get(i).getValue());
						mViews.put(i, newView);
					}
					
					showViewsSelectionDialog(views);
				}
			});
        	
        	JrSession.JrFs.getSubItemsAsync();
        }
    };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         
        setContentView(R.layout.activity_set_connection);
        
        mConnectionButton = (Button)findViewById(R.id.btnConnect);
        mConnectionButton.setOnClickListener(mConnectionButtonListener);
        
        Library library = JrSession.GetLibrary(this);
        if (library == null) return;
        
    	EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
    	EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
    	EditText txtPassword = (EditText)findViewById(R.id.txtPassword);
    	
    	((CheckBox)findViewById(R.id.chkLocalOnly)).setChecked(library.isLocalOnly());
    	
    	txtAccessCode.setText(library.getAccessCode());
    	if (library.getAuthKey() == null) return;
    	String decryptedUserAuth = new String(Base64.decode(library.getAuthKey(), Base64.DEFAULT));
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
				
				JrSession.GetLibrary(thisContext).setSelectedViews(mSelectedViews);
				
				JrSession.SaveSession(setConnection);
				
				JrSession.JrFs = new JrFileSystem(mSelectedViews);
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
