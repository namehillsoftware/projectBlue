package com.lasthopesoftware.bluewater.about;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.BuildConfig;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsActivityIntentBuilder;
import com.lasthopesoftware.bluewater.shared.android.view.ScaledWrapImageView;
import com.lasthopesoftware.resources.intents.IntentFactory;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

public class AboutActivity extends AppCompatActivity implements View.OnLongClickListener {

	private final BuildAboutTitle aboutTitleBuilder = new AboutTitleBuilder(this);

	private final CreateAndHold<Bitmap> lazyLogoBitmap = new AbstractSynchronousLazy<Bitmap>() {
		@Override
		protected Bitmap create() {
			return BitmapFactory.decodeResource(getResources(), R.drawable.music_canoe_hi_res_logo);
		}
	};

	private final CreateAndHold<HiddenSettingsActivityIntentBuilder> lazyHiddenSettingsActivityIntentBuilder = new AbstractSynchronousLazy<HiddenSettingsActivityIntentBuilder>() {
		@Override
		protected HiddenSettingsActivityIntentBuilder create() {
			return new HiddenSettingsActivityIntentBuilder(new IntentFactory(AboutActivity.this));
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		setTitle(aboutTitleBuilder.buildTitle());

		final TextView textView = findViewById(R.id.aboutDescription);
		textView.setText(
			String.format(
				getString(R.string.aboutAppText),
				getString(R.string.app_name),
				BuildConfig.VERSION_NAME,
				String.valueOf(BuildConfig.VERSION_CODE),
				getString(R.string.company_name),
				getString(R.string.copyright_year)));

		final RelativeLayout logoImageContainer = findViewById(R.id.logoImageContainer);
		logoImageContainer.setOnLongClickListener(this);

		final ScaledWrapImageView scaledWrapImageView = new ScaledWrapImageView(this);
		scaledWrapImageView.setImageBitmap(lazyLogoBitmap.getObject());

		logoImageContainer.addView(scaledWrapImageView);
	}

	@Override
	public boolean onLongClick(View v) {
		startActivity(lazyHiddenSettingsActivityIntentBuilder.getObject().buildHiddenSettingsIntent());
		return true;
	}
}
