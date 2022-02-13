package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.createViewModelLazy
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.databinding.ControlNowPlayingTopSheetBinding

class NowPlayingTopFragment : Fragment() {

	private val nowPlayingViewModel by createViewModelLazy()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val binding = DataBindingUtil.inflate<ControlNowPlayingTopSheetBinding>(
			inflater,
			R.layout.control_now_playing_top_sheet,
			container,
			true
		)

		return binding.nowPlayingTopSheet
	}
}
