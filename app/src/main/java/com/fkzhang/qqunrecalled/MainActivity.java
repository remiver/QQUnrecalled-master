package com.fkzhang.qqunrecalled;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_TONE_PICKER = 0;
    private MenuItem mMenuItemIcon;
    private TextView ringtone_name;
    private SettingsHelper mSettingsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSettingsHelper = new SettingsHelper(this,
                "com.fkzhang.qqunrecalled");

        final Switch enable_troopassistant_recall_notification =
                (Switch) findViewById(R.id.enable_troopassistant_recall_notification);
        enable_troopassistant_recall_notification.setChecked(mSettingsHelper
                .getBoolean("enable_troopassistant_recall_notification", false));
        enable_troopassistant_recall_notification.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mSettingsHelper.setBoolean("enable_troopassistant_recall_notification",
                                isChecked);
                    }
                });

        Switch enable_recall_notification =
                (Switch) findViewById(R.id.enable_recall_notification);
        enable_recall_notification.setChecked(mSettingsHelper
                .getBoolean("enable_recall_notification", true));
        enable_recall_notification.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mSettingsHelper.setBoolean("enable_recall_notification", isChecked);
                        if (!isChecked) {
                            enable_troopassistant_recall_notification.setChecked(false);
                        }
                    }
                });
        Switch show_content = (Switch) findViewById(R.id.show_content);
        show_content.setChecked(mSettingsHelper.getBoolean("show_content", false));
        show_content.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettingsHelper.setBoolean("show_content", isChecked);
            }
        });

        EditText recalled_message = (EditText) findViewById(R.id.editText);
        if (TextUtils.isEmpty(mSettingsHelper.getString("qq_recalled", null))) {
            mSettingsHelper.setString("qq_recalled",
                    getString(R.string.qq_recalled_msg_content));
        }
        recalled_message.setText(mSettingsHelper.getString("qq_recalled", "(Prevented)"));
        recalled_message.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String t = s.toString().trim();
                if (TextUtils.isEmpty(t))
                    return;
                mSettingsHelper.setString("qq_recalled", t);
            }
        });

        final TextView ringtone_select = (TextView) findViewById(R.id.ringtone_select);
        ringtone_name = (TextView) findViewById(R.id.ringtone_name);
        ringtone_select.setOnClickListener(this);
        ringtone_name.setOnClickListener(this);

        String uri = mSettingsHelper.getString("ringtone", "");
        if (TextUtils.isEmpty(uri)) {
            ringtone_name.setText(R.string.default_text);
        } else {
            Ringtone ringTone = RingtoneManager.getRingtone(getApplicationContext(), Uri.parse(uri));
            ringtone_name.setText(ringTone.getTitle(this));
        }
        Switch ringtone_switch = (Switch) findViewById(R.id.ringtone);
        ringtone_switch.setChecked(mSettingsHelper.getBoolean("ringtone_enable", false));
        ringtone_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettingsHelper.setBoolean("ringtone_enable", isChecked);
                if (isChecked) {
                    ringtone_select.setVisibility(View.VISIBLE);
                    ringtone_name.setVisibility(View.VISIBLE);
                } else {
                    ringtone_select.setVisibility(View.INVISIBLE);
                    ringtone_name.setVisibility(View.INVISIBLE);
                }
            }
        });

        Switch vibrate_switch = (Switch) findViewById(R.id.vibrate);
        vibrate_switch.setChecked(mSettingsHelper.getBoolean("vibrate_enable", false));
        vibrate_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettingsHelper.setBoolean("vibrate_enable", isChecked);
            }
        });

        if (!mSettingsHelper.getBoolean("ringtone_enable", false)) {
            ringtone_select.setVisibility(View.INVISIBLE);
            ringtone_name.setVisibility(View.INVISIBLE);
        }

//        final Intent intent = new Intent(this, SupportActivity.class);
//        findViewById(R.id.textView0).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(intent);
//            }
//        });

        mSettingsHelper.setString("qq_recalled_offline",
                getString(R.string.qq_recalled_offline));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenuItemIcon = menu.findItem(R.id.action_icon);
        setMenuIconTitle();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_icon:
                toggleLauncherIcon(!isIconEnabled());
                setMenuIconTitle();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setMenuIconTitle() {
        if (isIconEnabled()) {
            mMenuItemIcon.setTitle(R.string.hide_icon);
        } else {
            mMenuItemIcon.setTitle(R.string.show_icon);
        }
    }

    private void toggleLauncherIcon(boolean newValue) {
        PackageManager packageManager = this.getPackageManager();
        int state = newValue ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        packageManager.setComponentEnabledSetting(getIconComponentName(), state,
                PackageManager.DONT_KILL_APP);
    }

    private ComponentName getIconComponentName() {
        return new ComponentName(this, "com.fkzhang.qqunrecalled.MainActivity-Alias");
    }

    private boolean isIconEnabled() {
        return this.getPackageManager().getComponentEnabledSetting(getIconComponentName()) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    @Override
    public void onClick(View v) {
        if (!mSettingsHelper.getBoolean("ringtone_enable", false))
            return;

        String uri = mSettingsHelper.getString("ringtone", "");
        final Uri currentTone = TextUtils.isEmpty(uri) ? RingtoneManager
                .getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION) :
                Uri.parse(uri);
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_ringtone));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentTone);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        startActivityForResult(intent, REQUEST_TONE_PICKER);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_TONE_PICKER) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            Ringtone ringTone = RingtoneManager.getRingtone(getApplicationContext(), uri);
            ringtone_name.setText(ringTone.getTitle(this));
            mSettingsHelper.setString("ringtone", uri.toString());
        }
    }

}
