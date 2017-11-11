package com.lasthopesoftware.bluewater.about;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.BuildConfig;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.shared.android.view.ScaledWrapImageView;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

public class AboutActivity extends AppCompatActivity {

	private final BuildAboutTitle aboutTitleBuilder = new AboutTitleBuilder(this);

	private final CreateAndHold<Bitmap> lazyLogoBitmap = new AbstractSynchronousLazy<Bitmap>() {
		@Override
		protected Bitmap create() throws Exception {
			return BitmapFactory.decodeResource(getResources(), R.drawable.music_canoe_hi_res_logo);
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

		final ScaledWrapImageView scaledWrapImageView = new ScaledWrapImageView(this);
		scaledWrapImageView.setImageBitmap(lazyLogoBitmap.getObject());

		logoImageContainer.addView(scaledWrapImageView);
	}

	@Override
	protected void onDestroy() {
		if (lazyLogoBitmap.isCreated())
			lazyLogoBitmap.getObject().recycle();

		super.onDestroy();
	}
}
