package com.lasthopesoftware.bluewater.activities;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils.OnGetNowPlayingSetListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.IItem;
import com.lasthopesoftware.bluewater.data.service.objects.FileSystem;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;

public class SetConnection extends FragmentActivity {
	private Button mConnectionButton;
	private Context thisContext = this;
	private Library mLibrary;

	private OnClickListener mConnectionButtonListener = new OnClickListener() {
        public void onClick(View v) {
        	final EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
        	final EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
        	final EditText txtPassword = (EditText)findViewById(R.id.txtPassword);
        	
        	final Context _context = v.getContext();
        	
        	if (mLibrary == null) mLibrary = new Library();
        	
        	mLibrary.setAccessCode(txtAccessCode.getText().toString());
        	mLibrary.setAuthKey(Base64.encodeToString((txtUserName.getText().toString() + ":" + txtPassword.getText().toString()).getBytes(), Base64.DEFAULT).trim());
        	
        	mLibrary.setLocalOnly(((CheckBox)findViewById(R.id.chkLocalOnly)).isChecked());
		        	
        	mConnectionButton.setText(R.string.btn_connecting);
        	mConnectionButton.setEnabled(false);
        	
        	JrSession.SaveSession(_context, mLibrary, new ISimpleTask.OnCompleteListener<Void, Void, Library>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, Library> owner, Library result) {
					mConnectionButton.setText(R.string.lbl_connected);
					
					thisContext.startActivity(new Intent(_context, InstantiateSessionConnection.class));
				}
			});

        	
        }
    };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.activity_set_connection);
        getActionBar().setDisplayHomeAsUpEnabled(true);        
        
        mConnectionButton = (Button)findViewById(R.id.btnConnect);
        mConnectionButton.setOnClickListener(mConnectionButtonListener);
        
        JrSession.GetLibrary(this, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				if (result == null) return;
				
				mLibrary = result;
		    	EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
		    	EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
		    	EditText txtPassword = (EditText)findViewById(R.id.txtPassword);
		    	
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		final MenuItem nowPlayingItem = menu.findItem(R.id.menu_view_now_playing);
		nowPlayingItem.setVisible(false);
		ViewUtils.displayNowPlayingInMenu(this, new OnGetNowPlayingSetListener() {
			
			@Override
			public void onGetNowPlayingSetComplete(Boolean isSet) {
				nowPlayingItem.setVisible(isSet);
			}
		});
		menu.findItem(R.id.menu_connection_settings).setVisible(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return ViewUtils.handleNavMenuClicks(this, item);
	}
}
