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
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
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
import com.lasthopesoftware.bluewater.shared.LazyView;

import java.util.ArrayList;

public class EditServerSettingsActivity extends AppCompatActivity {
	public static final String serverIdExtra = EditServerSettingsActivity.class.getCanonicalName() + ".serverIdExtra";

	private Library library;

	private final LazyView<Button> saveButton = new LazyView<>(this, R.id.btnConnect);
	private final LazyView<EditText> txtAccessCode = new LazyView<>(this, R.id.txtAccessCode);
	private final LazyView<EditText> txtUserName = new LazyView<>(this, R.id.txtUserName);
	private final LazyView<EditText> txtPassword = new LazyView<>(this, R.id.txtPassword);
	private final LazyView<EditText> txtSyncPath = new LazyView<>(this, R.id.txtSyncPath);
	private final LazyView<CheckBox> chkLocalOnly = new LazyView<>(this, R.id.chkLocalOnly);
	private final LazyView<RadioGroup> rgSyncFileOptions = new LazyView<>(this, R.id.rgSyncFileOptions);
	private final LazyView<CheckBox> chkIsUsingExistingFiles = new LazyView<>(this, R.id.chkIsUsingExistingFiles);
	private final LazyView<CheckBox> chkIsUsingLocalConnectionForSync = new LazyView<>(this, R.id.chkIsUsingLocalConnectionForSync);

	private static final int permissionsRequestInteger = 1;

	private final OnClickListener connectionButtonListener = v -> {
        saveButton.getObject().setEnabled(false);

        if (library == null) {
            library = new Library();
            library.setNowPlayingId(-1);
        }

        library.setAccessCode(txtAccessCode.getObject().getText().toString());
        library.setAuthKey(Base64.encodeToString((txtUserName.getObject().getText().toString() + ":" + txtPassword.getObject().getText().toString()).getBytes(), Base64.DEFAULT).trim());
        library.setLocalOnly(chkLocalOnly.getObject().isChecked());
        library.setCustomSyncedFilesPath(txtSyncPath.getObject().getText().toString());
        switch (rgSyncFileOptions.getObject().getCheckedRadioButtonId()) {
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
        library.setIsUsingExistingFiles(chkIsUsingExistingFiles.getObject().isChecked());
        library.setIsSyncLocalConnectionsOnly(chkIsUsingLocalConnectionForSync.getObject().isChecked());

        final ArrayList<String> permissionsToRequest = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (library.isExternalReadAccessNeeded() && ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
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

        saveButton.getObject().setOnClickListener(connectionButtonListener);
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
			txtSyncPath.getObject().setText(externalFilesDir.getPath());

		final RadioGroup localSyncFileOptions = rgSyncFileOptions.getObject();
		localSyncFileOptions.check(R.id.rbPrivateToApp);

		localSyncFileOptions.setOnCheckedChangeListener((group, checkedId) -> txtSyncPath.getObject().setEnabled(checkedId == R.id.rbCustomLocation));

		final int libraryId = intent.getIntExtra(serverIdExtra, -1);
		LibrarySession.GetLibrary(this, libraryId, result -> {
			if (result == null) return;

			library = result;

			if (
					(library.isExternalReadAccessNeeded() && ContextCompat.checkSelfPermission(EditServerSettingsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) ||
							(library.isExternalWriteAccessNeeded() && ContextCompat.checkSelfPermission(EditServerSettingsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED)) {

				showPermissionsAlertDialog();
			}

			chkLocalOnly.getObject().setChecked(library.isLocalOnly());
			chkIsUsingExistingFiles.getObject().setChecked(library.isUsingExistingFiles());
			chkIsUsingLocalConnectionForSync.getObject().setChecked(library.isSyncLocalConnectionsOnly());

			final String customSyncPath = library.getCustomSyncedFilesPath();
			if (customSyncPath != null && !customSyncPath.isEmpty())
				txtSyncPath.getObject().setText(customSyncPath);

			switch (library.getSyncedFileLocation()) {
				case EXTERNAL:
					localSyncFileOptions.check(R.id.rbPublicLocation);
					break;
				case INTERNAL:
					localSyncFileOptions.check(R.id.rbPrivateToApp);
					break;
				case CUSTOM:
					localSyncFileOptions.check(R.id.rbCustomLocation);
					break;
			}

			txtAccessCode.getObject().setText(library.getAccessCode());
			if (library.getAuthKey() == null) return;

			final String decryptedUserAuth = new String(Base64.decode(library.getAuthKey(), Base64.DEFAULT));
			if (decryptedUserAuth.isEmpty()) return;

			final String[] userDetails = decryptedUserAuth.split(":", 2);
			txtUserName.getObject().setText(userDetails[0]);
			txtPassword.getObject().setText(userDetails[1] != null ? userDetails[1] : "");
		});
	}

	private void showPermissionsAlertDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setCancelable(false);
		builder
			.setTitle(getString(R.string.permissions_needed))
			.setMessage(getString(R.string.permissions_needed_launch_settings));

		builder.show();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode != permissionsRequestInteger) return;

		for (int grantResult : grantResults) {
			if (grantResult == PackageManager.PERMISSION_GRANTED) continue;

			Toast.makeText(this, R.string.permissions_must_be_granted_for_settings, Toast.LENGTH_LONG).show();
			saveButton.getObject().setEnabled(true);
			return;
		}

		saveLibraryAndFinish();
	}

	private void saveLibraryAndFinish() {
		LibrarySession.SaveLibrary(this, library, result -> {
			saveButton.getObject().setText(getText(R.string.btn_saved));
			finish();
		});
	}
}
