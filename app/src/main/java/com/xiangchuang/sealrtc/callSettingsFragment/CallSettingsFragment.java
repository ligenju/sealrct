package com.xiangchuang.sealrtc.callSettingsFragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xiangchuang.sealrtc.R;
import com.xiangchuang.sealrtc.utils.SessionManager;


public class CallSettingsFragment extends DialogFragment
        implements RadioGroup.OnCheckedChangeListener,
                CallSettingsPagerView.CallSettingsPagerListener {

    private static final String TAG = CallSettingsFragment.class.getSimpleName();
    RadioGroup rgCallSettingsTabs;
    CallSettingsPagerView pvCallSettingsPager;
    private CallSettingFragmentListener listener;

    public interface CallSettingFragmentListener {

        void onSwitchAudioOptions(boolean isOn);

        void onUploadClickEvents();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_AppCompat_Dialog);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        dialog.getWindow()
                .setLayout((int) (dm.widthPixels * 0.75f), (int) (dm.heightPixels * 0.75f));
        dialog.getWindow()
                .setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorBG)));
        dialog.setCanceledOnTouchOutside(true);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.fragment_call_settings, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        initView(layout);
        return layout;
    }

    public void setListener(CallSettingFragmentListener listener) {
        this.listener = listener;
    }

    private void initView(View layout) {

        rgCallSettingsTabs = layout.findViewById(R.id.rg_call_settings_tabs);
        pvCallSettingsPager = layout.findViewById(R.id.pv_call_settings_pager);
        rgCallSettingsTabs.setOnCheckedChangeListener(this);
        rgCallSettingsTabs.check(R.id.rb_call_settings_tab_audio);
        pvCallSettingsPager.setCallSettingsPagerListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {

        switch (radioGroup.getCheckedRadioButtonId()) {
            case R.id.rb_call_settings_tab_video:
                {
                    pvCallSettingsPager.showVideoPager();
                    break;
                }
            case R.id.rb_call_settings_tab_audio:
                {
                    Log.d(TAG, "onCheckedChanged: rb_call_settings_tab_audio");
                    pvCallSettingsPager.showAudioPager();
                    pvCallSettingsPager.setAudioOn(
                            SessionManager.getInstance()
                                    .getBoolean(
                                            "is_musicMode",
                                            getResources()
                                                    .getBoolean(R.bool.def_audio_music_mode)));
                    break;
                }

            case R.id.rb_call_settings_tab_other:
                {
                    Log.d(TAG, "onCheckedChanged: rb_call_settings_tab_other");
                    pvCallSettingsPager.showOtherPager();
                    break;
                }
            default:
                {
                }
        }
    }

    @Override
    public void onAudioSwitch(boolean isOn) {
        if (listener != null) {
            listener.onSwitchAudioOptions(isOn);
        }
    }

    @Override
    public void onUploadClick() {
        if (listener != null) {
            listener.onUploadClickEvents();
        }
    }
}
