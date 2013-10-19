package com.lasthopesoftware.bluewater.activities.fragments;

import com.lasthopesoftware.bluewater.R;

import android.app.ProgressDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;

public class WaitingForConnectionDialogFragment extends DialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String title = getString(R.string.lbl_connection_lost_title);
		String message = String.format(getString(R.string.lbl_waiting_for_connection), getString(R.string.app_name));
		
		return ProgressDialog.show(getActivity(), title, message, true, true, new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				// TODO Auto-generated method stub
				
			}
		});
	}
}
