package com.lasthopesoftware.jrmediastreamer;

import jrAccess.JrSession;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SetUpConnection extends FragmentActivity {

	private OnClickListener mConnectionButtonListener = new OnClickListener() {
        public void onClick(View v) {
        	EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
        	EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
        	EditText txtPassword = (EditText)findViewById(R.id.txtPassword);
        	
        	JrSession.AccessCode = txtAccessCode.getText().toString();
        	JrSession.UserAuthCode = Base64.encodeToString((txtUserName.getText().toString() + ":" + txtPassword.getText().toString()).getBytes(), Base64.DEFAULT).trim();
        	
        	JrSession.SaveSession(v.getContext());
        	
        	if (!JrSession.CreateSession(getSharedPreferences(JrSession.PREFS_FILE, 0))) return;
        	
        	Intent intent = new Intent(v.getContext(), BrowseLibrary.class);
        	startActivity(intent);
        }
    };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         
        setContentView(R.layout.activity_set_up_connection);

    	EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
    	EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
    	EditText txtPassword = (EditText)findViewById(R.id.txtPassword);
    	
    	txtAccessCode.setText(JrSession.AccessCode);
    	String decryptedUserAuth = new String(Base64.decode(JrSession.UserAuthCode, Base64.DEFAULT));
    	if (!decryptedUserAuth.isEmpty()) {
	    	String[] userDetails = decryptedUserAuth.split(":",2);
	    	txtUserName.setText(userDetails[0]);
	    	txtPassword.setText(userDetails[1] != null ? userDetails[1] : "");
    	}

    	Button connectionButton = (Button)findViewById(R.id.btnConnect);
    	connectionButton.setOnClickListener(mConnectionButtonListener);
	}
	
}
