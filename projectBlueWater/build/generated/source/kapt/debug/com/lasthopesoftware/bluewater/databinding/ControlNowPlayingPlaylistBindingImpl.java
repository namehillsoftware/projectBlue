package com.lasthopesoftware.bluewater.databinding;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ControlNowPlayingPlaylistBindingImpl extends ControlNowPlayingPlaylistBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.nowPlayingMiniControls, 7);
        sViewsWithIds.put(R.id.reducedNowPlayingButtonsContainer, 8);
        sViewsWithIds.put(R.id.miniPlayPause, 9);
        sViewsWithIds.put(R.id.closeNowPlayingList, 10);
        sViewsWithIds.put(R.id.nowPlayingListView, 11);
    }
    // views
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ControlNowPlayingPlaylistBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 12, sIncludes, sViewsWithIds));
    }
    private ControlNowPlayingPlaylistBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 5
            , (android.widget.ImageButton) bindings[10]
            , (android.widget.ImageButton) bindings[1]
            , (android.widget.ImageButton) bindings[2]
            , (android.widget.ProgressBar) bindings[6]
            , (android.widget.ImageButton) bindings[5]
            , (android.widget.ImageButton) bindings[4]
            , (android.widget.ViewFlipper) bindings[9]
            , (android.widget.RelativeLayout) bindings[0]
            , (androidx.recyclerview.widget.RecyclerView) bindings[11]
            , (android.widget.RelativeLayout) bindings[7]
            , (android.widget.LinearLayout) bindings[8]
            , (android.widget.ImageButton) bindings[3]
            );
        this.editNowPlayingList.setTag(null);
        this.finishEditNowPlayingList.setTag(null);
        this.miniNowPlayingBar.setTag(null);
        this.miniPause.setTag(null);
        this.miniPlay.setTag(null);
        this.nowPlayingBottomSheet.setTag(null);
        this.repeatButton.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x80L;
        }
        requestRebind();
    }

    @Override
    public boolean hasPendingBindings() {
        synchronized(this) {
            if (mDirtyFlags != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setVariable(int variableId, @Nullable Object variable)  {
        boolean variableSet = true;
        if (BR.vm == variableId) {
            setVm((com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel) variable);
        }
        else if (BR.playlistVm == variableId) {
            setPlaylistVm((com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.NowPlayingPlaylistViewModel) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setVm(@Nullable com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel Vm) {
        this.mVm = Vm;
        synchronized(this) {
            mDirtyFlags |= 0x20L;
        }
        notifyPropertyChanged(BR.vm);
        super.requestRebind();
    }
    public void setPlaylistVm(@Nullable com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.NowPlayingPlaylistViewModel PlaylistVm) {
        this.mPlaylistVm = PlaylistVm;
        synchronized(this) {
            mDirtyFlags |= 0x40L;
        }
        notifyPropertyChanged(BR.playlistVm);
        super.requestRebind();
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
            case 0 :
                return onChangeVmIsPlaying((kotlinx.coroutines.flow.StateFlow<java.lang.Boolean>) object, fieldId);
            case 1 :
                return onChangeVmFileDuration((kotlinx.coroutines.flow.StateFlow<java.lang.Integer>) object, fieldId);
            case 2 :
                return onChangeVmIsRepeating((kotlinx.coroutines.flow.StateFlow<java.lang.Boolean>) object, fieldId);
            case 3 :
                return onChangeVmFilePosition((kotlinx.coroutines.flow.StateFlow<java.lang.Integer>) object, fieldId);
            case 4 :
                return onChangePlaylistVmIsEditingPlaylistState((kotlinx.coroutines.flow.StateFlow<java.lang.Boolean>) object, fieldId);
        }
        return false;
    }
    private boolean onChangeVmIsPlaying(kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> VmIsPlaying, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x1L;
            }
            return true;
        }
        return false;
    }
    private boolean onChangeVmFileDuration(kotlinx.coroutines.flow.StateFlow<java.lang.Integer> VmFileDuration, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x2L;
            }
            return true;
        }
        return false;
    }
    private boolean onChangeVmIsRepeating(kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> VmIsRepeating, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x4L;
            }
            return true;
        }
        return false;
    }
    private boolean onChangeVmFilePosition(kotlinx.coroutines.flow.StateFlow<java.lang.Integer> VmFilePosition, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x8L;
            }
            return true;
        }
        return false;
    }
    private boolean onChangePlaylistVmIsEditingPlaylistState(kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> PlaylistVmIsEditingPlaylistState, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x10L;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        android.graphics.drawable.Drawable vmIsRepeatingRepeatButtonAndroidDrawableAvRepeatWhiteRepeatButtonAndroidDrawableAvNoRepeatWhite = null;
        java.lang.Boolean vmIsRepeatingGetValue = null;
        java.lang.Integer vmFilePositionGetValue = null;
        com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel vm = mVm;
        kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> vmIsPlaying = null;
        kotlinx.coroutines.flow.StateFlow<java.lang.Integer> vmFileDuration = null;
        boolean androidxDatabindingViewDataBindingSafeUnboxVmIsPlaying = false;
        java.lang.Boolean vmIsPlayingGetValue = null;
        kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> vmIsRepeating = null;
        kotlinx.coroutines.flow.StateFlow<java.lang.Integer> vmFilePosition = null;
        boolean androidxDatabindingViewDataBindingSafeUnboxVmIsRepeatingGetValue = false;
        boolean playlistVmIsEditingPlaylistState = false;
        boolean VmIsPlaying1 = false;
        kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> PlaylistVmIsEditingPlaylistState1 = null;
        boolean androidxDatabindingViewDataBindingSafeUnboxPlaylistVmIsEditingPlaylistState = false;
        java.lang.Boolean playlistVmIsEditingPlaylistStateGetValue = null;
        int androidxDatabindingViewDataBindingSafeUnboxVmFileDurationGetValue = 0;
        java.lang.Integer vmFileDurationGetValue = null;
        com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.NowPlayingPlaylistViewModel playlistVm = mPlaylistVm;
        boolean androidxDatabindingViewDataBindingSafeUnboxPlaylistVmIsEditingPlaylistStateGetValue = false;
        boolean androidxDatabindingViewDataBindingSafeUnboxVmIsPlayingGetValue = false;
        int androidxDatabindingViewDataBindingSafeUnboxVmFilePositionGetValue = 0;

        if ((dirtyFlags & 0xafL) != 0) {


            if ((dirtyFlags & 0xa1L) != 0) {

                    if (vm != null) {
                        // read vm.isPlaying
                        vmIsPlaying = vm.isPlaying();
                    }
                    androidx.databinding.ViewDataBindingKtx.updateStateFlowRegistration(this, 0, vmIsPlaying);


                    if (vmIsPlaying != null) {
                        // read vm.isPlaying.getValue()
                        vmIsPlayingGetValue = vmIsPlaying.getValue();
                    }


                    // read androidx.databinding.ViewDataBinding.safeUnbox(vm.isPlaying.getValue())
                    androidxDatabindingViewDataBindingSafeUnboxVmIsPlayingGetValue = androidx.databinding.ViewDataBinding.safeUnbox(vmIsPlayingGetValue);


                    // read !androidx.databinding.ViewDataBinding.safeUnbox(vm.isPlaying.getValue())
                    VmIsPlaying1 = !androidxDatabindingViewDataBindingSafeUnboxVmIsPlayingGetValue;


                    // read androidx.databinding.ViewDataBinding.safeUnbox(!androidx.databinding.ViewDataBinding.safeUnbox(vm.isPlaying.getValue()))
                    androidxDatabindingViewDataBindingSafeUnboxVmIsPlaying = androidx.databinding.ViewDataBinding.safeUnbox(VmIsPlaying1);
            }
            if ((dirtyFlags & 0xa2L) != 0) {

                    if (vm != null) {
                        // read vm.fileDuration
                        vmFileDuration = vm.getFileDuration();
                    }
                    androidx.databinding.ViewDataBindingKtx.updateStateFlowRegistration(this, 1, vmFileDuration);


                    if (vmFileDuration != null) {
                        // read vm.fileDuration.getValue()
                        vmFileDurationGetValue = vmFileDuration.getValue();
                    }


                    // read androidx.databinding.ViewDataBinding.safeUnbox(vm.fileDuration.getValue())
                    androidxDatabindingViewDataBindingSafeUnboxVmFileDurationGetValue = androidx.databinding.ViewDataBinding.safeUnbox(vmFileDurationGetValue);
            }
            if ((dirtyFlags & 0xa4L) != 0) {

                    if (vm != null) {
                        // read vm.isRepeating
                        vmIsRepeating = vm.isRepeating();
                    }
                    androidx.databinding.ViewDataBindingKtx.updateStateFlowRegistration(this, 2, vmIsRepeating);


                    if (vmIsRepeating != null) {
                        // read vm.isRepeating.getValue()
                        vmIsRepeatingGetValue = vmIsRepeating.getValue();
                    }


                    // read androidx.databinding.ViewDataBinding.safeUnbox(vm.isRepeating.getValue())
                    androidxDatabindingViewDataBindingSafeUnboxVmIsRepeatingGetValue = androidx.databinding.ViewDataBinding.safeUnbox(vmIsRepeatingGetValue);
                if((dirtyFlags & 0xa4L) != 0) {
                    if(androidxDatabindingViewDataBindingSafeUnboxVmIsRepeatingGetValue) {
                            dirtyFlags |= 0x200L;
                    }
                    else {
                            dirtyFlags |= 0x100L;
                    }
                }


                    // read androidx.databinding.ViewDataBinding.safeUnbox(vm.isRepeating.getValue()) ? @android:drawable/av_repeat_white : @android:drawable/av_no_repeat_white
                    vmIsRepeatingRepeatButtonAndroidDrawableAvRepeatWhiteRepeatButtonAndroidDrawableAvNoRepeatWhite = ((androidxDatabindingViewDataBindingSafeUnboxVmIsRepeatingGetValue) ? (androidx.appcompat.content.res.AppCompatResources.getDrawable(repeatButton.getContext(), R.drawable.av_repeat_white)) : (androidx.appcompat.content.res.AppCompatResources.getDrawable(repeatButton.getContext(), R.drawable.av_no_repeat_white)));
            }
            if ((dirtyFlags & 0xa8L) != 0) {

                    if (vm != null) {
                        // read vm.filePosition
                        vmFilePosition = vm.getFilePosition();
                    }
                    androidx.databinding.ViewDataBindingKtx.updateStateFlowRegistration(this, 3, vmFilePosition);


                    if (vmFilePosition != null) {
                        // read vm.filePosition.getValue()
                        vmFilePositionGetValue = vmFilePosition.getValue();
                    }


                    // read androidx.databinding.ViewDataBinding.safeUnbox(vm.filePosition.getValue())
                    androidxDatabindingViewDataBindingSafeUnboxVmFilePositionGetValue = androidx.databinding.ViewDataBinding.safeUnbox(vmFilePositionGetValue);
            }
        }
        if ((dirtyFlags & 0xd0L) != 0) {



                if (playlistVm != null) {
                    // read playlistVm.isEditingPlaylistState
                    PlaylistVmIsEditingPlaylistState1 = playlistVm.isEditingPlaylistState();
                }
                androidx.databinding.ViewDataBindingKtx.updateStateFlowRegistration(this, 4, PlaylistVmIsEditingPlaylistState1);


                if (PlaylistVmIsEditingPlaylistState1 != null) {
                    // read playlistVm.isEditingPlaylistState.getValue()
                    playlistVmIsEditingPlaylistStateGetValue = PlaylistVmIsEditingPlaylistState1.getValue();
                }


                // read androidx.databinding.ViewDataBinding.safeUnbox(playlistVm.isEditingPlaylistState.getValue())
                androidxDatabindingViewDataBindingSafeUnboxPlaylistVmIsEditingPlaylistStateGetValue = androidx.databinding.ViewDataBinding.safeUnbox(playlistVmIsEditingPlaylistStateGetValue);


                // read !androidx.databinding.ViewDataBinding.safeUnbox(playlistVm.isEditingPlaylistState.getValue())
                playlistVmIsEditingPlaylistState = !androidxDatabindingViewDataBindingSafeUnboxPlaylistVmIsEditingPlaylistStateGetValue;


                // read androidx.databinding.ViewDataBinding.safeUnbox(!androidx.databinding.ViewDataBinding.safeUnbox(playlistVm.isEditingPlaylistState.getValue()))
                androidxDatabindingViewDataBindingSafeUnboxPlaylistVmIsEditingPlaylistState = androidx.databinding.ViewDataBinding.safeUnbox(playlistVmIsEditingPlaylistState);
        }
        // batch finished
        if ((dirtyFlags & 0xd0L) != 0) {
            // api target 1

            com.lasthopesoftware.bluewater.shared.android.viewmodels.BindingsKt.toggleVisibility(this.editNowPlayingList, androidxDatabindingViewDataBindingSafeUnboxPlaylistVmIsEditingPlaylistState);
            com.lasthopesoftware.bluewater.shared.android.viewmodels.BindingsKt.toggleVisibility(this.finishEditNowPlayingList, androidxDatabindingViewDataBindingSafeUnboxPlaylistVmIsEditingPlaylistStateGetValue);
        }
        if ((dirtyFlags & 0xa8L) != 0) {
            // api target 1

            this.miniNowPlayingBar.setProgress(androidxDatabindingViewDataBindingSafeUnboxVmFilePositionGetValue);
        }
        if ((dirtyFlags & 0xa2L) != 0) {
            // api target 1

            this.miniNowPlayingBar.setMax(androidxDatabindingViewDataBindingSafeUnboxVmFileDurationGetValue);
        }
        if ((dirtyFlags & 0xa1L) != 0) {
            // api target 1

            com.lasthopesoftware.bluewater.shared.android.viewmodels.BindingsKt.toggleVisibility(this.miniPause, androidxDatabindingViewDataBindingSafeUnboxVmIsPlayingGetValue);
            com.lasthopesoftware.bluewater.shared.android.viewmodels.BindingsKt.toggleVisibility(this.miniPlay, androidxDatabindingViewDataBindingSafeUnboxVmIsPlaying);
        }
        if ((dirtyFlags & 0xa4L) != 0) {
            // api target 1

            androidx.databinding.adapters.ImageViewBindingAdapter.setImageDrawable(this.repeatButton, vmIsRepeatingRepeatButtonAndroidDrawableAvRepeatWhiteRepeatButtonAndroidDrawableAvNoRepeatWhite);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): vm.isPlaying
        flag 1 (0x2L): vm.fileDuration
        flag 2 (0x3L): vm.isRepeating
        flag 3 (0x4L): vm.filePosition
        flag 4 (0x5L): playlistVm.isEditingPlaylistState
        flag 5 (0x6L): vm
        flag 6 (0x7L): playlistVm
        flag 7 (0x8L): null
        flag 8 (0x9L): androidx.databinding.ViewDataBinding.safeUnbox(vm.isRepeating.getValue()) ? @android:drawable/av_repeat_white : @android:drawable/av_no_repeat_white
        flag 9 (0xaL): androidx.databinding.ViewDataBinding.safeUnbox(vm.isRepeating.getValue()) ? @android:drawable/av_repeat_white : @android:drawable/av_no_repeat_white
    flag mapping end*/
    //end
}