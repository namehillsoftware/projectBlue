package com.lasthopesoftware.bluewater.servers;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;

import java.io.File;

public class EditServerActivity extends FragmentActivity {

	public static final String serverIdExtra = EditServerActivity.class.getCanonicalName() + ".serverIdExtra";

	private Button mConnectionButton;
	private Library mLibrary;

	private EditText mTxtAccessCode;
	private EditText mTxtUserName;
	private EditText mTxtPassword;
	private EditText mTxtSyncPath;
	private CheckBox mChkLocalOnly;
	private RadioGroup mRgSyncFileOptions;
	private CheckBox mChkIsUsingExistingFiles;

	private OnClickListener mConnectionButtonListener = new OnClickListener() {
        public void onClick(View v) {

        	final Context _context = v.getContext();
        	
        	if (mLibrary == null) {
        		mLibrary = new Library();
        		mLibrary.setNowPlayingId(-1);
        	}

	        mLibrary.setAccessCode(mTxtAccessCode.getText().toString());
        	mLibrary.setAuthKey(Base64.encodeToString((mTxtUserName.getText().toString() + ":" + mTxtPassword.getText().toString()).getBytes(), Base64.DEFAULT).trim());
        	mLibrary.setLocalOnly(mChkLocalOnly.isChecked());
	        mLibrary.setCustomSyncedFilesPath(mTxtSyncPath.getText().toString());
	        switch (mRgSyncFileOptions.getCheckedRadioButtonId()) {
		        case R.id.rbPublicLocation:
			        mLibrary.setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL);
			        break;
		        case R.id.rbPrivateToApp:
			        mLibrary.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL);
			        break;
		        case R.id.rbCustomLocation:
			        mLibrary.setSyncedFileLocation(Library.SyncedFileLocation.CUSTOM);
			        break;
	        }
	        mLibrary.setIsUsingExistingFiles(mChkIsUsingExistingFiles.isChecked());

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
		mRgSyncFileOptions = (RadioGroup) findViewById(R.id.rgSyncFileOptions);
		mChkIsUsingExistingFiles = (CheckBox) findViewById(R.id.chkIsUsingExistingFiles);

		final File externalFilesDir = Environment.getExternalStorageDirectory();
		if (externalFilesDir != null)
			mTxtSyncPath.setText(externalFilesDir.getPath());

		mRgSyncFileOptions.check(R.id.rbPrivateToApp);

		mRgSyncFileOptions.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				mTxtSyncPath.setEnabled(checkedId == R.id.rbCustomLocation);
			}
		});

		mTxtAccessCode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				mConnectionButton.setEnabled(v.getText().length() > 0);
				return false;
			}
		});

		final int libraryId = getIntent().getIntExtra(serverIdExtra, -1);
		LibrarySession.GetLibrary(this, libraryId, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				if (result == null) return;

				mLibrary = result;

				mChkLocalOnly.setChecked(mLibrary.isLocalOnly());
				mChkIsUsingExistingFiles.setChecked(mLibrary.isUsingExistingFiles());

				final String customSyncPath = mLibrary.getCustomSyncedFilesPath();
				if (customSyncPath != null && !customSyncPath.isEmpty())
					mTxtSyncPath.setText(customSyncPath);

				switch (mLibrary.getSyncedFileLocation()) {
					case EXTERNAL:
						mRgSyncFileOptions.check(R.id.rbPublicLocation);
						break;
					case INTERNAL:
						mRgSyncFileOptions.check(R.id.rbPrivateToApp);
						break;
					case CUSTOM:
						mRgSyncFileOptions.check(R.id.rbCustomLocation);
						break;
				}

				mTxtAccessCode.setText(mLibrary.getAccessCode());
				if (mLibrary.getAuthKey() == null) return;

				final String decryptedUserAuth = new String(Base64.decode(mLibrary.getAuthKey(), Base64.DEFAULT));
				if (decryptedUserAuth.isEmpty()) return;

				final String[] userDetails = decryptedUserAuth.split(":", 2);
				mTxtUserName.setText(userDetails[0]);
				mTxtPassword.setText(userDetails[1] != null ? userDetails[1] : "");
			}
		});
        
	}
}
