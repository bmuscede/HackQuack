package com.uwaterloo.bmuscede.hackquack;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class StartActivity extends AppCompatActivity implements UICallback {
    private Intent hqScrapper;
    private HQScraper scrapperService;
    private boolean bound;

    public static final String AUTH_CODE_LOC = "authCode";
    public static final String AUTH_CODE_STATUS_LOC = "statusCode";
    public static final int NO_CODE = -1;
    public static final int UKN_CODE = 0;
    public static final int BAD_CODE = 1;
    public static final int GOOD_CODE = 2;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // cast the IBinder and get HQScraper instance
            HQScraper.LocalBinder binder = (HQScraper.LocalBinder) service;
            scrapperService = binder.getService();
            bound = true;
            scrapperService.registerUICallback(StartActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            scrapperService.registerUICallback(null);
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        //Sets up the authorization code.
        EditText authCode = (EditText) findViewById(R.id.txtAuthCode);
        authCode.setText(getSavedAuthCode(AUTH_CODE_LOC));

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
    }

    public void resetHQButton(){
        Button appStart = (Button) findViewById(R.id.btnAction);
        stopService(hqScrapper);
        appStart.setText(R.string.start_btn);
    }

    public void setAuthStatusCode(int code){
        //Saves the file's code.
        saveAuthCode(AUTH_CODE_STATUS_LOC, String.valueOf(code));
    }

    public void setTickerStatus() {
        TextView lblStatus = (TextView) findViewById(R.id.lblAuthInfo);
        ImageView imgStatus = (ImageView) findViewById(R.id.imgAuthInfo);

        //Loads in the status code.
        int statusCode = Integer.parseInt(getSavedAuthCode(AUTH_CODE_STATUS_LOC));
        if (statusCode == GOOD_CODE){
            //Change status code.
            lblStatus.setText(R.string.auth_code_good);
            lblStatus.setTextColor(Color.GREEN);

            //Change picture.
            imgStatus.setImageResource(R.drawable.check);
        } else if (statusCode == UKN_CODE) {
            //Change status code.
            lblStatus.setText(R.string.auth_code_ukn);
            lblStatus.setTextColor(Color.BLUE);

            //Change picture.
            imgStatus.setImageResource(R.drawable.question);
        } else {
            //Change picture and color.
            imgStatus.setImageResource(R.drawable.error);
            lblStatus.setTextColor(Color.RED);

            if (statusCode == NO_CODE) lblStatus.setText(R.string.auth_code_ns);
            else if (statusCode == BAD_CODE) lblStatus.setText(R.string.auth_code_bad);
        }
    }

    protected String getSavedAuthCode(String file) {
        FileInputStream inputStream;
        String results = "";

        try {
            inputStream = openFileInput(file);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            results = sb.toString();
        } catch (FileNotFoundException e) {
            if (file.equals(AUTH_CODE_STATUS_LOC)) return String.valueOf(NO_CODE);
            return "";
        } catch (Exception e){
            //TODO Add error message.
            e.printStackTrace();
        }

        return results.trim();
    }

    protected void saveAuthCode(String file, String savedCode){
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(file, Context.MODE_PRIVATE);
            outputStream.write(savedCode.getBytes());
            outputStream.close();
        } catch (Exception e) {
            //TODO Add error message.
            e.printStackTrace();
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
                bindService(hqScrapper, serviceConnection, Context.BIND_AUTO_CREATE);

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
            String savedAuthCode = getSavedAuthCode(AUTH_CODE_LOC);
            if (authCode.equals(savedAuthCode)) return;

            //Saves the authorization code.
            saveAuthCode(AUTH_CODE_LOC, authCode);
            if (s.toString().trim().equals("")) saveAuthCode(AUTH_CODE_STATUS_LOC, String.valueOf(NO_CODE));
            else saveAuthCode(AUTH_CODE_STATUS_LOC, String.valueOf(UKN_CODE));

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
}

