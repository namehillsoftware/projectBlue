package com.lasthopesoftware.bluewater.activities;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.service.objects.JrFileSystem;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;

public class SetConnection extends FragmentActivity {
	private Button mConnectionButton;
	private Context thisContext = this;

	private OnClickListener mConnectionButtonListener = new OnClickListener() {
        public void onClick(View v) {
        	EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
        	EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
        	EditText txtPassword = (EditText)findViewById(R.id.txtPassword);
        	
        	final Context _context = v.getContext();
        	
        	Library library = JrSession.GetLibrary(_context);

        	if (library != null) {
        		library.setAccessCode(txtAccessCode.getText().toString());
	        	library.setAuthKey(Base64.encodeToString((txtUserName.getText().toString() + ":" + txtPassword.getText().toString()).getBytes(), Base64.DEFAULT).trim());
	        	
	        	library.setLocalOnly(((CheckBox)findViewById(R.id.chkLocalOnly)).isChecked());
        	}
        	
        	
        	mConnectionButton.setText(R.string.btn_connecting);
        	mConnectionButton.setEnabled(false);
        	
        	JrSession.SaveSession(_context, new ISimpleTask.OnCompleteListener<Void, Void, Library>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, Library> owner, Library result) {
					if (JrSession.ChooseLibrary(_context, result.getId()) == null) return;
		        	
		        	if (JrSession.JrFs == null) JrSession.JrFs = new JrFileSystem();
		        	
		        	JrSession.JrFs.setOnItemsCompleteListener(new OnCompleteListener<List<IJrItem<?>>>() {
						
						@Override
						public void onComplete(ISimpleTask<String, Void, List<IJrItem<?>>> owner, List<IJrItem<?>> result) {
							if (result == null) return;
							
							if (result.size() == 0)
								Toast.makeText(thisContext, "This library doesn't contain any views", Toast.LENGTH_LONG).show();
							
							JrSession.JrFs = new JrFileSystem(result.get(0).getKey());
							JrSession.GetLibrary(_context).setSelectedView(result.get(0).getKey());
							JrSession.SaveSession(_context);
							
							Intent intent = new Intent(_context, BrowseLibrary.class);
							thisContext.startActivity(intent);
						}
					});
		        	
		        	JrSession.JrFs.getSubItemsAsync();
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
}
