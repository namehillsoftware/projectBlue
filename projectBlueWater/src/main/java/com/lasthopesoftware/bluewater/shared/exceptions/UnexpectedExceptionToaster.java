package com.lasthopesoftware.bluewater.shared.exceptions;

import android.content.Context;
import android.widget.Toast;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

public class UnexpectedExceptionToaster implements ImmediateResponse<Throwable, Void> {

	private final Context context;

	public UnexpectedExceptionToaster(Context context) {
		this.context = context;
	}

	@Override
	public Void respond(Throwable error) {
		Toast.makeText(context, "An unexpected error occurred! The error was " + error.getClass().getName(), Toast.LENGTH_SHORT).show();

		return null;
	}
}
