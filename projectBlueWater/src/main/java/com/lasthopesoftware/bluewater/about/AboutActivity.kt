package com.lasthopesoftware.bluewater.about

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.View.OnLongClickListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsActivityIntentBuilder
import com.lasthopesoftware.bluewater.shared.android.ui.theme.theme.ProjectBlueTheme
import com.lasthopesoftware.resources.intents.IntentFactory

class AboutActivity : ComponentActivity(), OnLongClickListener {
	private val aboutTitleBuilder by lazy { AboutTitleBuilder(this) }
	private val logoBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.project_blue_logo_circular) }
	private val hiddenSettingsActivityIntentBuilder by lazy { HiddenSettingsActivityIntentBuilder(IntentFactory(this@AboutActivity)) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			ProjectBlueTheme {
				Text(getString(R.string.aboutAppText).format(
					getString(R.string.app_name),
					BuildConfig.VERSION_NAME,
					BuildConfig.VERSION_CODE,
					getString(R.string.company_name),
					getString(R.string.copyright_year)
				))
			}
		}

//		setContentView(R.layout.activity_about)
//
//		title = aboutTitleBuilder.buildTitle()
//
//		val textView = findViewById<TextView>(R.id.aboutDescription)
//		textView.text = getString(R.string.aboutAppText).format(
//			getString(R.string.app_name),
//			BuildConfig.VERSION_NAME,
//			BuildConfig.VERSION_CODE,
//			getString(R.string.company_name),
//			getString(R.string.copyright_year)
//		)
//
//		val logoImageContainer = findViewById<RelativeLayout>(R.id.logoImageContainer)
//		logoImageContainer.setOnLongClickListener(this)
//
//		val scaledWrapImageView = ScaledWrapImageView(this)
//		scaledWrapImageView.setImageBitmap(logoBitmap)
//		logoImageContainer.addView(scaledWrapImageView)
	}

	override fun onLongClick(v: View): Boolean {
		startActivity(hiddenSettingsActivityIntentBuilder.buildHiddenSettingsIntent())
		return true
	}
}
