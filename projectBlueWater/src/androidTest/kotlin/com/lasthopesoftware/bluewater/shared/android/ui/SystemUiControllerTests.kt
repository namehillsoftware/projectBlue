package com.lasthopesoftware.bluewater.shared.android.ui

import android.os.Build
import android.view.View
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.lasthopesoftware.bluewater.shared.android.ui.components.SystemUiController
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberSystemUiController
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Ignore("Does not run on build server")
class SystemUiControllerTests {
	@get:Rule
	val rule = createAndroidComposeRule<ComponentActivity>()

	private lateinit var window: Window
	private lateinit var contentView: View

	@Before
	fun setup() {
		window = rule.activityRule.scenario.withActivity { it.window }
		contentView = rule.activityRule.scenario.withActivity {
			it.findViewById(android.R.id.content)!!
		}

		if (Build.VERSION.SDK_INT >= 29) {
			// On API 29+, the system can modify the bar colors to maintain contrast.
			// We disable that here to make it simple to assert expected values
			rule.activityRule.scenario.onActivity {
				window.apply {
					isNavigationBarContrastEnforced = false
					isStatusBarContrastEnforced = false
				}
			}
		}
	}

	@Test
	fun statusBarColor() {
		rule.setContent {
			// Create an systemUiController and set the status bar color
			val systemUiController = rememberSystemUiController()
			SideEffect {
				systemUiController.setStatusBarColor(Color.Blue, darkIcons = false)
			}
		}

		// Assert that the color was set
		assertThat(Color(window.statusBarColor)).isEqualTo(Color.Blue)
	}

	@Test
	fun navigationBarColor() {
		rule.setContent {
			// Now create an systemUiController and set the navigation bar color
			val systemUiController = rememberSystemUiController()
			SideEffect {
				systemUiController.setNavigationBarColor(Color.Green, darkIcons = false)
			}
		}

		assertThat(Color(window.navigationBarColor)).isEqualTo(Color.Green)
	}

	@Test
	fun systemBarColor() {
		rule.setContent {
			// Now create an systemUiController and set the system bar colors
			val systemUiController = rememberSystemUiController()
			SideEffect {
				systemUiController.setSystemBarsColor(Color.Red, darkIcons = false)
			}
		}

		// Assert that the colors were set
		assertThat(Color(window.statusBarColor)).isEqualTo(Color.Red)
		assertThat(Color(window.navigationBarColor)).isEqualTo(Color.Red)
	}

	@Test
	@SdkSuppress(minSdkVersion = 23)
	fun statusBarIcons_native() {
		// Now create an systemUiController and set the status bar with dark icons
		rule.setContent {
			val systemUiController = rememberSystemUiController()
			SideEffect {
				systemUiController.setStatusBarColor(Color.White, darkIcons = true) {
					// Here we can provide custom logic to 'darken' the color to maintain contrast.
					// We return red just to assert below.
					Color.Red
				}
			}
		}

		// Assert that the colors were darkened color is not used
		assertThat(Color(window.statusBarColor)).isEqualTo(Color.White)

		// Assert that the system applied the native light icons
		rule.activityRule.scenario.onActivity {
			val windowInsetsController = WindowCompat.getInsetsController(window, contentView)
			assertThat(windowInsetsController.isAppearanceLightStatusBars).isTrue()
		}
	}

	@Test
	@SdkSuppress(minSdkVersion = 26)
	fun navigationBar_native() {
		// Now create an systemUiController and set the navigation bar with dark icons
		rule.setContent {
			val systemUiController = rememberSystemUiController()
			SideEffect {
				systemUiController.setNavigationBarColor(Color.White, darkIcons = true) {
					// Here we can provide custom logic to 'darken' the color to maintain contrast.
					// We return red just to assert below.
					Color.Red
				}
			}
		}

		// Assert that the colors were darkened color is not used
		assertThat(Color(window.navigationBarColor)).isEqualTo(Color.White)

		// Assert that the system applied the native light icons
		rule.activityRule.scenario.onActivity {
			val windowInsetsController = WindowCompat.getInsetsController(window, contentView)
			assertThat(windowInsetsController.isAppearanceLightNavigationBars).isTrue()
		}
	}

	@Test
	@SdkSuppress(minSdkVersion = 29)
	fun navigationBar_contrastEnforced() {
		lateinit var systemUiController: SystemUiController

		rule.setContent {
			systemUiController = rememberSystemUiController()
		}

		rule.activityRule.scenario.onActivity {
			// Assert that the contrast is not enforced initially
			assertThat(systemUiController.isNavigationBarContrastEnforced).isFalse()

			// and set the navigation bar with dark icons and enforce contrast
			systemUiController.setNavigationBarColor(
				Color.Transparent,
				darkIcons = true,
				navigationBarContrastEnforced = true
			) {
				// Here we can provide custom logic to 'darken' the color to maintain contrast.
				// We return red just to assert below.
				Color.Red
			}

			// Assert that the colors were darkened color is not used
			assertThat(Color(window.navigationBarColor)).isEqualTo(Color.Transparent)

			// Assert that the system applied the contrast enforced property
			assertThat(window.isNavigationBarContrastEnforced).isTrue()

			// Assert that the controller reflects that the contrast is enforced
			assertThat(systemUiController.isNavigationBarContrastEnforced).isTrue()
		}
	}

	@Suppress("DEPRECATION")
	@Test
	@SdkSuppress(minSdkVersion = 30) // TODO: https://issuetracker.google.com/issues/189366125
	fun systemBarsBehavior_showBarsByTouch() {
		lateinit var systemUiController: SystemUiController

		rule.setContent {
			systemUiController = rememberSystemUiController()
		}

		rule.activityRule.scenario.onActivity {
			systemUiController.systemBarsBehavior =
				WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
		}

		assertThat(WindowCompat.getInsetsController(window, contentView).systemBarsBehavior)
			.isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH)
	}

	@Test
	@SdkSuppress(minSdkVersion = 30) // TODO: https://issuetracker.google.com/issues/189366125
	fun systemBarsBehavior_showBarsBySwipe() {
		lateinit var systemUiController: SystemUiController

		rule.setContent {
			systemUiController = rememberSystemUiController()
		}

		rule.activityRule.scenario.onActivity {
			systemUiController.systemBarsBehavior =
				WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
		}

		assertThat(WindowCompat.getInsetsController(window, contentView).systemBarsBehavior)
			.isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
	}

	@Test
	@SdkSuppress(minSdkVersion = 30) // TODO: https://issuetracker.google.com/issues/189366125
	fun systemBarsBehavior_showTransientBarsBySwipe() {
		lateinit var systemUiController: SystemUiController

		rule.setContent {
			systemUiController = rememberSystemUiController()
		}

		rule.activityRule.scenario.onActivity {
			systemUiController.systemBarsBehavior =
				WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
		}

		assertThat(WindowCompat.getInsetsController(window, contentView).systemBarsBehavior)
			.isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
	}
}
