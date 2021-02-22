package com.my.mylibrary;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

public class AudioMixActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    private Fragment[] fragments = new Fragment[2];
    private int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_mix);

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radio_button);
        radioGroup.setOnCheckedChangeListener(this);

        FragmentManager fm = getSupportFragmentManager();
        fragments[0] = fm.findFragmentById(R.id.fm_audio_mix);
        fragments[1] = fm.findFragmentById(R.id.fm_audio_effect);
        fm.beginTransaction().hide(fragments[1]).commit();
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.img_btn_close || id == R.id.v_place_holder) {
            performClose();
        }
    }

    private void performClose() {
        AudioMixFragment.alive = false;
        finish();
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        switchFragment();
    }

    private void switchFragment() {
        Fragment currentFragment = fragments[index()];
        Fragment nextFragment = fragments[next()];
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.hide(currentFragment);
        transaction.show(nextFragment);
        transaction.commit();
    }

    private int index() {
        return index % fragments.length;
    }

    private int next() {
        index++;
        return index();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.mix_slide_down);
    }
}
