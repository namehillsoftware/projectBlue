package com.lasthopesoftware.bluewater.servers;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;

public class EditServerActivity extends FragmentActivity {
	private Button mConnectionButton;
	private Library mLibrary;

	private EditText mTxtAccessCode;
	private EditText mTxtUserName;
	private EditText mTxtPassword;
	private EditText mTxtSyncPath;
	private CheckBox mChkLocalOnly;

	private OnClickListener mConnectionButtonListener = new OnClickListener() {
        public void onClick(View v) {

        	final Context _context = v.getContext();
        	
        	if (mLibrary == null) {
        		mLibrary = new Library();
        		mLibrary.setNowPlayingId(-1);
        	}

	        mLibrary.setSyncedFilesPath(mTxtSyncPath.getText().toString());
        	mLibrary.setAccessCode(mTxtAccessCode.getText().toString());
        	mLibrary.setAuthKey(Base64.encodeToString((mTxtUserName.getText().toString() + ":" + mTxtPassword.getText().toString()).getBytes(), Base64.DEFAULT).trim());
        	mLibrary.setLocalOnly(mChkLocalOnly.isChecked());
		        	
        	mConnectionButton.setEnabled(false);
        	
        	LibrarySession.SaveLibrary(_context, mLibrary, new ISimpleTask.OnCompleteListener<Void, Void, Library>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, Library> owner, Library result) {
					mConnectionButton.setText(getText(R.string.btn_saved));
					finish();
				}
			});
        }
    };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.activity_edit_server);

		mConnectionButton = (Button)findViewById(R.id.btnConnect);
        mConnectionButton.setOnClickListener(mConnectionButtonListener);
		mTxtAccessCode = (EditText)findViewById(R.id.txtAccessCode);
		mTxtUserName = (EditText)findViewById(R.id.txtUserName);
		mTxtPassword = (EditText)findViewById(R.id.txtPassword);
		mTxtSyncPath = (EditText) findViewById(R.id.txtSyncPath);
		mChkLocalOnly = (CheckBox) findViewById(R.id.chkLocalOnly);
        
        LibrarySession.GetLibrary(this, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				if (result == null) return;
				
				mLibrary = result;

				mTxtSyncPath.setText(mLibrary.getSyncedFilesPath());
				mChkLocalOnly.setChecked(mLibrary.isLocalOnly());

				mTxtAccessCode.setText(mLibrary.getAccessCode());
		    	if (mLibrary.getAuthKey() == null) return;

		    	final String decryptedUserAuth = new String(Base64.decode(mLibrary.getAuthKey(), Base64.DEFAULT));
		    	if (decryptedUserAuth.isEmpty()) return;

		        final String[] userDetails = decryptedUserAuth.split(":",2);
			    mTxtUserName.setText(userDetails[0]);
			    mTxtPassword.setText(userDetails[1] != null ? userDetails[1] : "");
			}
        });
        
	}
}
