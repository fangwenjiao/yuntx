package com.yuntongxun.ecdemo.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.yuntongxun.ecdemo.R;
import com.yuntongxun.ecdemo.common.utils.ECPreferenceSettings;
import com.yuntongxun.ecdemo.common.utils.ECPreferences;
import com.yuntongxun.ecdemo.ui.ECSuperActivity;

import java.io.InvalidClassException;

public class EditConfigureActivity extends ECSuperActivity implements View.OnClickListener{

    public static final String EXTRA_EDIT_TITLE = "edit_title";
    public static final String EXTRA_EDIT_HINT = "edit_hint";
    private int mSettingType;
    private EditText mEdittext;
    private ECPreferenceSettings mSettings;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettingType = getIntent().getIntExtra("setting_type" , -1);
        String title = "";
        if(mSettingType == SettingsActivity.CONFIG_TYPE_APPKEY) {
            title = getString(R.string.edit_appkey);
            mSettings = ECPreferenceSettings.SETTINGS_APPKEY;
        } else if (mSettingType == SettingsActivity.CONFIG_TYPE_TOKEN) {
            title = getString(R.string.edit_token);
            mSettings = ECPreferenceSettings.SETTINGS_TOKEN;
        } else if (mSettingType == SettingsActivity.CONFIG_TYPE_SERVERIP) {
            title = getString(R.string.edit_serverip);
            // mSettings = ECPreferenceSettings.SETTINGS_SERVERIP;
        } else {
            mSettingType = -1;
            title = getIntent().getStringExtra("edit_title");
        }

        getTopBarView().setTopBarToStatus(1, R.drawable.topbar_back_bt,
                R.drawable.btn_style_green, null,
                getString(R.string.dialog_ok_button),
                title, null, this);
        initView();
    }

    private void initView() {
        mEdittext = (EditText) findViewById(R.id.content);

        if(mSettingType != -1) {
            String config = getConfig(mSettings);
            mEdittext.setText(config);
            mEdittext.setSelection(mEdittext.getText().length());
            return ;
        }
        String defaultData = getIntent().getStringExtra("edit_default_data");
        if(!TextUtils.isEmpty(defaultData)) {
            mEdittext.setText(defaultData);
            mEdittext.setSelection(defaultData.length());
        } else if (getIntent().hasExtra(EXTRA_EDIT_HINT)) {
            mEdittext.setHint(getIntent().getStringExtra(EXTRA_EDIT_HINT));
        }
    }

    private String getConfig(ECPreferenceSettings settings) {
        SharedPreferences sharedPreferences = ECPreferences.getSharedPreferences();
        String value = sharedPreferences.getString(settings.getId() , (String)settings.getDefaultValue());
        return value;
    }

    private void saveSettings(ECPreferenceSettings settings) {
        String settingsValue = mEdittext.getText().toString().trim();
        try {
            ECPreferences.savePreference(settings ,settingsValue,true);
        } catch (InvalidClassException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_edit_configure;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_left:
                hideSoftKeyboard();
                finish();
                break;
            case R.id.text_right:
                hideSoftKeyboard();
                if(mSettingType == -1) {
                    Intent intent = new Intent();
                    intent.putExtra("result_data" , mEdittext.getText().toString().toString());
                    setResult(RESULT_OK ,intent);
                }  else {
                    saveSettings(mSettings);
                    setResult(RESULT_OK);
                }
                finish();
                break;
            default:
                break;
        }
    }
}
