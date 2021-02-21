package com.my.mylibrary;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import cn.rongcloud.rtc.api.IAudioEffectManager;
import cn.rongcloud.rtc.api.IAudioEffectManager.ILoadingStateCallback;
import cn.rongcloud.rtc.api.RCRTCEngine;

public class AudioEffectFragment extends Fragment implements OnSeekBarChangeListener,
        OnClickListener, OnCheckedChangeListener, IAudioEffectManager.IStateObserver {

    private static final String EFFECT_0 = "file:///android_asset/effect0.mp3";
    private static final String EFFECT_1 = "file:///android_asset/effect1.mp3";
    private static final String EFFECT_2 = "file:///android_asset/effect2.mp3";

    private static final String[] effects = {EFFECT_0, EFFECT_1, EFFECT_2};
    public static final boolean[] preloaded = {false, false, false};
    private ImageButton[] playPauseBtns = new ImageButton[3];
    private ImageButton[] stopBtns = new ImageButton[3];

    private Context context;
    private SeekBar sb_global_vol;
    private TextView tv_global_vol;
    private EditText tvCyclicCount;
    private Button btnStopAll;
    private Handler uiHandler;

    public static boolean alive = false;

    private IAudioEffectManager effectManager;
    public static int loopCount = 1;

    public AudioEffectFragment() { }

    public static AudioEffectFragment newInstance() {
        return new AudioEffectFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AudioEffectFragment.alive = true;
        this.context = getContext();
        effectManager = RCRTCEngine.getInstance().getAudioEffectManager();
        uiHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio_effect, container, false);
        playPauseBtns[0] = view.findViewById(R.id.img_btn_effect0_play_pause);
        playPauseBtns[1] = view.findViewById(R.id.img_btn_effect1_play_pause);
        playPauseBtns[2] = view.findViewById(R.id.img_btn_effect2_play_pause);

        stopBtns[0] = view.findViewById(R.id.img_btn_effect0_stop);
        stopBtns[1] = view.findViewById(R.id.img_btn_effect1_stop);
        stopBtns[2] = view.findViewById(R.id.img_btn_effect2_stop);

        sb_global_vol = view.findViewById(R.id.sb_global_vol);
        tv_global_vol = view.findViewById(R.id.tv_global_vol);
        tvCyclicCount = view.findViewById(R.id.tv_cyclic_count);
        btnStopAll = view.findViewById(R.id.btn_stop_all);

        SeekBar sb_effect0 = view.findViewById(R.id.sb_effect0_vol);
        SeekBar sb_effect1 = view.findViewById(R.id.sb_effect1_vol);
        SeekBar sb_effect2 = view.findViewById(R.id.sb_effect2_vol);
        sb_effect0.setOnSeekBarChangeListener(this);
        sb_effect1.setOnSeekBarChangeListener(this);
        sb_effect2.setOnSeekBarChangeListener(this);
        sb_global_vol.setOnSeekBarChangeListener(this);

        Switch swEffect0 = view.findViewById(R.id.switch_effect0);
        Switch swEffect1 = view.findViewById(R.id.switch_effect1);
        Switch swEffect2 = view.findViewById(R.id.switch_effect2);
        if (preloaded[0]) {
            swEffect0.setChecked(true);
        }
        if (preloaded[1]) {
            swEffect1.setChecked(true);
        }
        if (preloaded[2]) {
            swEffect2.setChecked(true);
        }
        swEffect0.setOnCheckedChangeListener(this);
        swEffect1.setOnCheckedChangeListener(this);
        swEffect2.setOnCheckedChangeListener(this);

        playPauseBtns[0].setOnClickListener(this);
        playPauseBtns[1].setOnClickListener(this);
        playPauseBtns[2].setOnClickListener(this);
        stopBtns[0].setOnClickListener(this);
        stopBtns[1].setOnClickListener(this);
        stopBtns[2].setOnClickListener(this);
        btnStopAll.setOnClickListener(this);

        int effect0Vol = effectManager.getEffectVolume(0);
        int effect1Vol = effectManager.getEffectVolume(1);
        int effect2Vol = effectManager.getEffectVolume(2);
        int effectsVol = effectManager.getEffectsVolume();
        sb_effect0.setProgress(effect0Vol > 0 ? effect0Vol : 100);
        sb_effect1.setProgress(effect1Vol > 0 ? effect1Vol : 100);
        sb_effect2.setProgress(effect2Vol > 0 ? effect2Vol : 100);
        sb_global_vol.setProgress(effectsVol);

        tvCyclicCount.setText(String.valueOf(loopCount));
        tvCyclicCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s)) {
                    loopCount = Integer.parseInt(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        effectManager.registerStateObserver(this);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        effectManager.unregisterStateObserver(this);
    }

    @Override
    public void onEffectFinished(final int effectId) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                playPauseBtns[effectId].setSelected(false);
                stopBtns[effectId].setEnabled(false);
            }
        });
    }

    @Override
    public void onCheckedChanged(final CompoundButton compoundButton, boolean checked) {
        int id = compoundButton.getId();
        if (id == R.id.switch_effect0) {
            if (checked) {
                loadEffect(0, compoundButton);
                preloaded[0] = true;
            } else {
                effectManager.unloadEffect(0);
                preloaded[0] = false;
            }
        } else if (id == R.id.switch_effect1) {
            if (checked) {
                loadEffect(1, compoundButton);
                preloaded[1] = true;
            } else {
                effectManager.unloadEffect(1);
                preloaded[1] = false;
            }
        } else if (id == R.id.switch_effect2) {
            if (checked) {
                loadEffect(2, compoundButton);
                preloaded[2] = true;
            } else {
                effectManager.unloadEffect(2);
                preloaded[2] = true;
            }
        }
    }

    private void loadEffect(int effectId, final CompoundButton btn) {
        Toast.makeText(context, "wait for loading...", Toast.LENGTH_SHORT).show();
        effectManager.preloadEffect(effects[effectId], effectId, new ILoadingStateCallback() {
            @Override
            public void complete(int error) {
                if (error == 0) {
                    Toast.makeText(context, "load success", Toast.LENGTH_SHORT).show();
                } else {
                    btn.toggle();
                }
            }
        });
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.img_btn_effect0_play_pause) {
            playOrPauseEffect((ImageButton) view, 0);
        } else if (id == R.id.img_btn_effect1_play_pause) {
            playOrPauseEffect((ImageButton) view, 1);
        } else if (id == R.id.img_btn_effect2_play_pause) {
            playOrPauseEffect((ImageButton) view, 2);
        } else if (id == R.id.img_btn_effect0_stop) {
            stopEffect(0);
        } else if (id == R.id.img_btn_effect1_stop) {
            stopEffect(1);
        } else if (id == R.id.img_btn_effect2_stop) {
            stopEffect(2);
        } else if (id == R.id.btn_stop_all) {
            stopAllEffects();
        }
    }

    private void playOrPauseEffect(ImageButton btn, int effectId) {
        boolean selected = btn.isSelected();
        btn.setSelected(!selected);
        stopBtns[effectId].setEnabled(true);
        if (selected) {
            if (effectManager.pauseEffect(effectId) != 0) {
                btn.setSelected(true);
            }
        } else {
            if (effectManager.resumeEffect(effectId) < 0) {
                int volume = effectManager.getEffectVolume(effectId);
                if (effectManager.playEffect(effectId, loopCount, volume) != 0) {
                    Toast.makeText(context, "play failed", Toast.LENGTH_SHORT).show();
                    btn.setSelected(false);
                }
            }
        }
    }

    private void stopEffect(int effectId) {
        effectManager.stopEffect(effectId);
    }

    private void stopAllEffects() {
        effectManager.stopAllEffects();
        for (ImageButton imageButton : playPauseBtns) {
            imageButton.setSelected(false);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int volume = progress;
        int id = seekBar.getId();
        if (id == R.id.sb_effect0_vol) {
            effectManager.setEffectVolume(0, volume);
        } else if (id == R.id.sb_effect1_vol) {
            effectManager.setEffectVolume(1, volume);
        } else if (id == R.id.sb_effect2_vol) {
            effectManager.setEffectVolume(2, volume);
        } else if (id == R.id.sb_global_vol) {
            effectManager.setEffectsVolume(volume);
            tv_global_vol.setText(String.valueOf(volume));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // do nothing
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // do nothing
    }

}
