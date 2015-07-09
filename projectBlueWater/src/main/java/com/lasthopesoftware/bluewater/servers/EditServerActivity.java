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

	private OnClickListener mConnectionButtonListener = new OnClickListener() {
        public void onClick(View v) {
        	final EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
        	final EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
        	final EditText txtPassword = (EditText)findViewById(R.id.txtPassword);
        	
        	final Context _context = v.getContext();
        	
        	if (mLibrary == null) {
        		mLibrary = new Library();
        		mLibrary.setNowPlayingId(-1);
        	}
        	
        	mLibrary.setAccessCode(txtAccessCode.getText().toString());
        	mLibrary.setAuthKey(Base64.encodeToString((txtUserName.getText().toString() + ":" + txtPassword.getText().toString()).getBytes(), Base64.DEFAULT).trim());
        	
        	mLibrary.setLocalOnly(((CheckBox)findViewById(R.id.chkLocalOnly)).isChecked());
		        	
        	mConnectionButton.setText(R.string.btn_connecting);
        	mConnectionButton.setEnabled(false);
        	
        	LibrarySession.SaveLibrary(_context, mLibrary, new ISimpleTask.OnCompleteListener<Void, Void, Library>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, Library> owner, Library result) {
					mConnectionButton.setText("Saved!");
					finish();
				}
			});
        }
    };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.activity_configure_library);

        mConnectionButton = (Button)findViewById(R.id.btnConnect);
        mConnectionButton.setOnClickListener(mConnectionButtonListener);
        
        LibrarySession.GetLibrary(this, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				if (result == null) return;
				
				mLibrary = result;
		    	final EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
		    	final EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
		    	final EditText txtPassword = (EditText)findViewById(R.id.txtPassword);
		    	
		    	((CheckBox)findViewById(R.id.chkLocalOnly)).setChecked(mLibrary.isLocalOnly());
		    	
		    	txtAccessCode.setText(mLibrary.getAccessCode());
		    	if (mLibrary.getAuthKey() == null) return;
		    	String decryptedUserAuth = new String(Base64.decode(mLibrary.getAuthKey(), Base64.DEFAULT));
		    	if (!decryptedUserAuth.isEmpty()) {
			    	String[] userDetails = decryptedUserAuth.split(":",2);
			    	txtUserName.setText(userDetails[0]);
			    	txtPassword.setText(userDetails[1] != null ? userDetails[1] : "");
		    	}
			}
        });
        
	}
}
