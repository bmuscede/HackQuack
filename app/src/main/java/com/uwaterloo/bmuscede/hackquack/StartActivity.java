package com.uwaterloo.bmuscede.hackquack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import static com.uwaterloo.bmuscede.hackquack.CodeManager.AUTH_CODE_LOC;

public class StartActivity extends AppCompatActivity {
    private Intent hqScrapper;
    private Context curContext = null;
    public static BroadcastReceiver receiver;

    public static final String BROADCAST_STRING = "com.uwaterloo.bmuscede.HqScrap";
    public static final String BROADCAST_TYPE = "type";
    public static final String BROADCAST_TYPE_1 = "reset";
    public static final String BROADCAST_TYPE_2 = "ticker";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        curContext = this;

        //Creates a broadcast receiver.
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String type = intent.getStringExtra(BROADCAST_TYPE);
                if (type == BROADCAST_TYPE_1) resetHQButton();
                else if (type == BROADCAST_TYPE_2) setTickerStatus();
            }
        };

        //Sets up the authorization code.
        EditText authCode = (EditText) findViewById(R.id.txtAuthCode);
        authCode.setText(CodeManager.getSavedCode(this, AUTH_CODE_LOC));

        //Adds button listener.
        Button appStart = (Button) findViewById(R.id.btnAction);
        appStart.setOnClickListener(new AppStartListener());

        //Adds authorization code listener.
        authCode.addTextChangedListener(new AuthCodeListener());

        //Last check for contents.
        if (authCode.getText().toString().length() == 0){
            appStart.setEnabled(false);
        } else {
            appStart.setEnabled(true);
        }

        //Sets the status ticker at the bottom.
        setTickerStatus();

        //Sets the checkbox value.
        setSaveQStatus();

        //Add a listener for checkbox.
        Switch chkSwitch = (Switch) findViewById(R.id.chkSaveQ);
        chkSwitch.setOnCheckedChangeListener(new CheckChangeListener());
    }

    public void generateToast(String toastMsg){
        Context context = getApplicationContext();
        CharSequence text = toastMsg;
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
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

    public void setTickerStatus() {
        TextView lblStatus = (TextView) findViewById(R.id.lblAuthInfo);
        ImageView imgStatus = (ImageView) findViewById(R.id.imgAuthInfo);

        //Loads in the status code.
        int statusCode = Integer.parseInt(
                CodeManager.getSavedCode(this, CodeManager.AUTH_CODE_STATUS_LOC));
        if (statusCode == CodeManager.GOOD_CODE){
            //Change status code.
            lblStatus.setText(R.string.auth_code_good);
            lblStatus.setTextColor(Color.GREEN);

            //Change picture.
            imgStatus.setImageResource(R.drawable.check);
        } else if (statusCode == CodeManager.UKN_CODE) {
            //Change status code.
            lblStatus.setText(R.string.auth_code_ukn);
            lblStatus.setTextColor(Color.BLUE);

            //Change picture.
            imgStatus.setImageResource(R.drawable.question);
        } else {
            //Change picture and color.
            imgStatus.setImageResource(R.drawable.error);
            lblStatus.setTextColor(Color.RED);

            if (statusCode == CodeManager.NO_CODE)
                lblStatus.setText(R.string.auth_code_ns);
            else if (statusCode == CodeManager.BAD_CODE)
                lblStatus.setText(R.string.auth_code_bad);
        }
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

    protected class AuthCodeListener implements TextWatcher{
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
                generateToast("Saving questions to " + CodeManager.HQ_Q_SAVE);
            } else {
                //First, save the auth code.
                CodeManager.saveCode(curContext, CodeManager.CHK_CODE_STATUS_LOC,
                        String.valueOf(CodeManager.CHK_OFF));
            }
        }
    }
}

