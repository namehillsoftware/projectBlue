package com.lasthopesoftware.bluewater;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;

public class AboutActivity extends AppCompatActivity {

	private final LazyViewFinder<Toolbar> lazyToolbar = new LazyViewFinder<>(this, R.id.toolbar);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		setSupportActionBar(lazyToolbar.findView());

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

}
