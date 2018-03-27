package com.uwaterloo.bmuscede.settings;

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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.uwaterloo.bmuscede.evaluate.hEvaluate;
import com.uwaterloo.bmuscede.hackquack.CodeManager;
import com.uwaterloo.bmuscede.hackquack.R;
import com.uwaterloo.bmuscede.hackquack.StartActivity;

import static com.uwaterloo.bmuscede.hackquack.CodeManager.AUTH_CODE_LOC;
import static com.uwaterloo.bmuscede.hackquack.CodeManager.generateToast;

public class hqSettings extends AppCompatActivity {
    private Context curContext = null;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hq_settings);
        curContext = this;

        //Manages toolbar settings.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView nView = findViewById(R.id.nav_view);
        nView.getMenu().getItem(2).setChecked(true);
        nView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        //TODO: Figure out if we can change states validly.

                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();

                        //See which item was tapped.
                        if (menuItem.getItemId() == R.id.hq_menu) {
                            Intent hqIntent = new Intent(curContext, StartActivity.class);
                            hqIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivityForResult(hqIntent, 0);
                        } else if (menuItem.getItemId() == R.id.gs_menu) {
                            Intent evIntent = new Intent(curContext, evSettings.class);
                            evIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivityForResult(evIntent, 0);
                        } else if (menuItem.getItemId() == R.id.he_menu) {
                            Intent heIntent = new Intent(curContext, hEvaluate.class);
                            heIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivityForResult(heIntent, 0);
                        }

                        return true;
                    }
                });

        //Sets up the authorization code.
        EditText authCode = (EditText) findViewById(R.id.txtAuthCode);
        authCode.setText(CodeManager.getSavedCode(this, AUTH_CODE_LOC));
        setTickerStatus();

        //Adds authorization code listener.
        authCode.addTextChangedListener(new AuthCodeListener());

        //Add a listener for checkbox.
        Switch chkSwitch = (Switch) findViewById(R.id.chkSaveQ);
        setSaveQStatus();

        //Adds a switch listener.
        chkSwitch.setOnCheckedChangeListener(new CheckChangeListener());
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

    public void setSaveQStatus() {
        Switch chkSwitch = (Switch) findViewById(R.id.chkSaveQ);

        //Loads in the checkbox state.
        int chkCode = Integer.parseInt(
                CodeManager.getSavedCode(this, CodeManager.CHK_CODE_STATUS_LOC));
        if (chkCode == CodeManager.CHK_OFF){
            chkSwitch.setChecked(false);
        } else {
            chkSwitch.setChecked(true);
        }
    }

    public void setTickerStatus() {
        TextView lblStatus = (TextView) findViewById(R.id.lblStatusCode);
        ImageView imgStatus = (ImageView) findViewById(R.id.imgOne);
        ImageView imgStatus2 = (ImageView) findViewById(R.id.imgTwo);

        //Loads in the status code.
        int statusCode = Integer.parseInt(
                CodeManager.getSavedCode(this, CodeManager.AUTH_CODE_STATUS_LOC));
        if (statusCode == CodeManager.GOOD_CODE){
            //Change status code.
            lblStatus.setText(R.string.auth_code_good);
            lblStatus.setTextColor(Color.GREEN);

            //Change picture.
            imgStatus.setImageResource(R.drawable.check);
            imgStatus2.setImageResource(R.drawable.check);
        } else if (statusCode == CodeManager.UKN_CODE) {
            //Change status code.
            lblStatus.setText(R.string.auth_code_ukn);
            lblStatus.setTextColor(Color.BLUE);

            //Change picture.
            imgStatus.setImageResource(R.drawable.question);
            imgStatus2.setImageResource(R.drawable.question);
        } else {
            //Change picture and color.
            imgStatus.setImageResource(R.drawable.error);
            imgStatus2.setImageResource(R.drawable.error);
            lblStatus.setTextColor(Color.RED);

            if (statusCode == CodeManager.NO_CODE)
                lblStatus.setText(R.string.auth_code_ns);
            else if (statusCode == CodeManager.BAD_CODE)
                lblStatus.setText(R.string.auth_code_bad);
        }
    }

    protected class AuthCodeListener implements TextWatcher {
        public void afterTextChanged(Editable s) {
            //Get the text in the listener.
            String authCode = s.toString();

            //Get the saved string.
            String savedAuthCode = CodeManager.getSavedCode(curContext, CodeManager.AUTH_CODE_LOC);
            if (authCode.equals(savedAuthCode)) return;

            //Saves the authorization code.
            CodeManager.saveCode(curContext, CodeManager.AUTH_CODE_LOC, authCode);
            if (s.toString().trim().equals(""))
                CodeManager.saveCode(curContext, CodeManager.AUTH_CODE_STATUS_LOC,
                        String.valueOf(CodeManager.NO_CODE));
            else
                CodeManager.saveCode(curContext, CodeManager.AUTH_CODE_STATUS_LOC,
                        String.valueOf(CodeManager.UKN_CODE));

            //Enables the button if the text isn't empty.
            Button appStart = (Button) findViewById(R.id.btnAction);
            if (authCode.trim().equals("")){
                appStart.setEnabled(false);
            } else {
                appStart.setEnabled(true);
            }

            //Sets the status ticker at the bottom.
            setTickerStatus();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }


    protected class CheckChangeListener implements Switch.OnCheckedChangeListener {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            //Check the status.
            if (isChecked){
                //First, save the auth code.
                CodeManager.saveCode(curContext, CodeManager.CHK_CODE_STATUS_LOC,
                        String.valueOf(CodeManager.CHK_ON));
                generateToast(curContext, "Saving questions to " + CodeManager.HQ_Q_SAVE);
            } else {
                //First, save the auth code.
                CodeManager.saveCode(curContext, CodeManager.CHK_CODE_STATUS_LOC,
                        String.valueOf(CodeManager.CHK_OFF));
            }
        }
    }
}
