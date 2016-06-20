package com.lasthopesoftware.bluewater.servers.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.permissions.ExternalStorageReadPermissionsArbitrator;
import com.lasthopesoftware.permissions.IExternalStorageReadPermissionsArbitrator;

import java.util.ArrayList;

public class EditServerSettingsActivity extends AppCompatActivity {
	public static final String serverIdExtra = EditServerSettingsActivity.class.getCanonicalName() + ".serverIdExtra";

	private Library library;

	private Button saveButton;
	private EditText txtAccessCode;
	private EditText txtUserName;
	private EditText txtPassword;
	private EditText txtSyncPath;
	private CheckBox chkLocalOnly;
	private RadioGroup rgSyncFileOptions;
	private CheckBox chkIsUsingExistingFiles;
	private CheckBox chkIsUsingLocalConnectionForSync;
	private IExternalStorageReadPermissionsArbitrator externalStorageReadPermissionsArbitrator;

	private static final int permissionsRequestInteger = 1;

	private final OnClickListener connectionButtonListener = v -> {
        saveButton.setEnabled(false);

        if (library == null) {
            library = new Library();
            library.setNowPlayingId(-1);
        }

        library.setAccessCode(txtAccessCode.getText().toString());
        library.setAuthKey(Base64.encodeToString((txtUserName.getText().toString() + ":" + txtPassword.getText().toString()).getBytes(), Base64.DEFAULT).trim());
        library.setLocalOnly(chkLocalOnly.isChecked());
        library.setCustomSyncedFilesPath(txtSyncPath.getText().toString());
        switch (rgSyncFileOptions.getCheckedRadioButtonId()) {
	        case R.id.rbPublicLocation:
		        library.setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL);
		        break;
	        case R.id.rbPrivateToApp:
		        library.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL);
		        break;
	        case R.id.rbCustomLocation:
		        library.setSyncedFileLocation(Library.SyncedFileLocation.CUSTOM);
		        break;
        }
        library.setIsUsingExistingFiles(chkIsUsingExistingFiles.isChecked());
        library.setIsSyncLocalConnectionsOnly(chkIsUsingLocalConnectionForSync.isChecked());

        final ArrayList<String> permissionsToRequest = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (library.isExternalReadAccessNeeded() && !externalStorageReadPermissionsArbitrator.isExternalStorageReadPermissionsGranted())
		        permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);

	        if (library.isExternalWriteAccessNeeded() && ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		        permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

	        if (permissionsToRequest.size() > 0) {
		        final String[] permissionsToRequestArray = permissionsToRequest.toArray(new String[permissionsToRequest.size()]);
		        ActivityCompat.requestPermissions(EditServerSettingsActivity.this, permissionsToRequestArray, permissionsRequestInteger);

		        return;
	        }
        }

		saveLibraryAndFinish();
    };

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.activity_edit_server_settings);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		saveButton = (Button)findViewById(R.id.btnConnect);
        saveButton.setOnClickListener(connectionButtonListener);
		txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);
		txtUserName = (EditText)findViewById(R.id.txtUserName);
		txtPassword = (EditText)findViewById(R.id.txtPassword);
		txtSyncPath = (EditText) findViewById(R.id.txtSyncPath);
		chkLocalOnly = (CheckBox) findViewById(R.id.chkLocalOnly);
		rgSyncFileOptions = (RadioGroup) findViewById(R.id.rgSyncFileOptions);
		chkIsUsingExistingFiles = (CheckBox) findViewById(R.id.chkIsUsingExistingFiles);
		chkIsUsingLocalConnectionForSync = (CheckBox) findViewById(R.id.chkIsUsingLocalConnectionForSync);
		externalStorageReadPermissionsArbitrator = new ExternalStorageReadPermissionsArbitrator(this);
	}

	@Override
	protected void onStart() {
		super.onStart();

		initializeLibrary(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		initializeLibrary(intent);
	}

	private void initializeLibrary(Intent intent) {
		final java.io.File externalFilesDir = Environment.getExternalStorageDirectory();
		if (externalFilesDir != null)
			txtSyncPath.setText(externalFilesDir.getPath());

		rgSyncFileOptions.check(R.id.rbPrivateToApp);

		rgSyncFileOptions.setOnCheckedChangeListener((group, checkedId) -> txtSyncPath.setEnabled(checkedId == R.id.rbCustomLocation));

		final int libraryId = intent.getIntExtra(serverIdExtra, -1);
		LibrarySession.GetLibrary(this, libraryId, result -> {
			if (result == null) return;

			library = result;

			chkLocalOnly.setChecked(library.isLocalOnly());
			chkIsUsingExistingFiles.setChecked(library.isUsingExistingFiles());
			chkIsUsingLocalConnectionForSync.setChecked(library.isSyncLocalConnectionsOnly());

			final String customSyncPath = library.getCustomSyncedFilesPath();
			if (customSyncPath != null && !customSyncPath.isEmpty())
				txtSyncPath.setText(customSyncPath);

			switch (library.getSyncedFileLocation()) {
				case EXTERNAL:
					rgSyncFileOptions.check(R.id.rbPublicLocation);
					break;
				case INTERNAL:
					rgSyncFileOptions.check(R.id.rbPrivateToApp);
					break;
				case CUSTOM:
					rgSyncFileOptions.check(R.id.rbCustomLocation);
					break;
			}

			txtAccessCode.setText(library.getAccessCode());
			if (library.getAuthKey() == null) return;

			final String decryptedUserAuth = new String(Base64.decode(library.getAuthKey(), Base64.DEFAULT));
			if (decryptedUserAuth.isEmpty()) return;

			final String[] userDetails = decryptedUserAuth.split(":", 2);
			txtUserName.setText(userDetails[0]);
			txtPassword.setText(userDetails[1] != null ? userDetails[1] : "");
		});
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode != permissionsRequestInteger) return;

		for (int grantResult : grantResults) {
			if (grantResult == PackageManager.PERMISSION_GRANTED) continue;

			Toast.makeText(this, R.string.permissions_must_be_granted_for_settings, Toast.LENGTH_LONG).show();
			saveButton.setEnabled(true);
			return;
		}

		saveLibraryAndFinish();
	}

	private void saveLibraryAndFinish() {
		LibrarySession.SaveLibrary(this, library, result -> {
			saveButton.setText(getText(R.string.btn_saved));
			finish();
		});
	}
}
