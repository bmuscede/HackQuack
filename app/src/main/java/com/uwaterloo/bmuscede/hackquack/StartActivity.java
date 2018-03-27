package com.uwaterloo.bmuscede.hackquack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.uwaterloo.bmuscede.evaluate.hEvaluate;
import com.uwaterloo.bmuscede.settings.evSettings;
import com.uwaterloo.bmuscede.settings.hqSettings;

import static com.uwaterloo.bmuscede.hackquack.CodeManager.AUTH_CODE_LOC;
import static com.uwaterloo.bmuscede.hackquack.CodeManager.AUTH_CODE_STATUS_LOC;

public class StartActivity extends AppCompatActivity {
    private Intent hqScrapper;
    private Context curContext = null;
    public static BroadcastReceiver receiver;
    private DrawerLayout mDrawerLayout;

    public static final String BROADCAST_STRING = "com.uwaterloo.bmuscede.HqScrap";
    public static final String BROADCAST_TYPE = "type";
    public static final String BROADCAST_TYPE_1 = "reset";
    public static final String BROADCAST_TYPE_2 = "ticker";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        curContext = this;

        //Manages toolbar settings.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView nView = findViewById(R.id.nav_view);
        nView.getMenu().getItem(0).setChecked(true);
        nView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        //TODO: Figure out if we can change states validly.

                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();

                        //See which item was tapped.
                        if (menuItem.getItemId() == R.id.he_menu) {
                            Intent heIntent = new Intent(curContext, hEvaluate.class);
                            heIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivityForResult(heIntent, 0);
                        } else if (menuItem.getItemId() == R.id.gs_menu) {
                            Intent evIntent = new Intent(curContext, evSettings.class);
                            evIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivityForResult(evIntent, 0);
                        } else if (menuItem.getItemId() == R.id.hqS_menu) {
                            Intent hqSIntent = new Intent(curContext, hqSettings.class);
                            hqSIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivityForResult(hqSIntent, 0);
                        }

                        return true;
                    }
                });

        //Creates a broadcast receiver.
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String type = intent.getStringExtra(BROADCAST_TYPE);
                if (type == BROADCAST_TYPE_1) resetHQButton();
                else if (type == BROADCAST_TYPE_2) setWarningStatus();
            }
        };

        //Adds button listener.
        Button appStart = (Button) findViewById(R.id.btnAction);
        appStart.setOnClickListener(new AppStartListener());

        //Last check for contents.
        setWarningStatus();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void resetHQButton(){
        Button appStart = (Button) findViewById(R.id.btnAction);
        stopService(hqScrapper);
        appStart.setText(R.string.start_btn);
    }

    public void setAuthStatusCode(int code){
        //Saves the file's code.
        CodeManager.saveCode(this, CodeManager.AUTH_CODE_STATUS_LOC, String.valueOf(code));
    }

    public void setWarningStatus(){
        //Check if the authcode is valid.
        String authCode = CodeManager.getSavedCode(this, AUTH_CODE_LOC);
        Button appStart = (Button) findViewById(R.id.btnAction);

        TextView lblMsg = (TextView) findViewById(R.id.lblError);
        if (authCode.length() == 0){
            lblMsg.setText(getString(R.string.error_msg));
            lblMsg.setTextColor(Color.RED);
            lblMsg.setVisibility(TextView.VISIBLE);
            appStart.setEnabled(false);
        } else {
            int code = Integer.parseInt(CodeManager.getSavedCode(this, AUTH_CODE_STATUS_LOC));
            if (code == CodeManager.BAD_CODE){
                lblMsg.setText(getString(R.string.warning_msg));
                lblMsg.setTextColor(Color.RED);
                lblMsg.setVisibility(TextView.VISIBLE);
            }
            appStart.setEnabled(true);
        }
    }

    protected class AppStartListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            //Gets the button.
            Button curButton = (Button) v;

            //Figures out what state we're in.
            if (curButton.getText().equals(getString(R.string.start_btn))){
                hqScrapper = new Intent(StartActivity.this, HQScraper.class);

                startService(hqScrapper);
                curButton.setText(R.string.stop_btn);
            } else {
                stopService(hqScrapper);
                curButton.setText(R.string.start_btn);
            }
        }
    }
}

