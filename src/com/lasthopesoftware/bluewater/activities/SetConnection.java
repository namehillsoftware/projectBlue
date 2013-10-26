package com.lasthopesoftware.bluewater.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.objects.JrSession;

public class SetConnection extends FragmentActivity {

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
        	
        	Intent intent = new Intent(v.getContext(), SelectLibrary.class);
        	startActivity(intent);
        }
    };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         
        setContentView(R.layout.activity_set_connection);
        
        Button connectionButton = (Button)findViewById(R.id.btnConnect);
    	connectionButton.setOnClickListener(mConnectionButtonListener);
        
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
}
