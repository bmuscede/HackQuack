package com.uwaterloo.bmuscede.hackquack;

import android.content.Context;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

/**
 * Created by bmuscede on 25/03/18.
 */

public class CodeManager {
    public static final String HQ_Q_SAVE = "HackQuack/hqQuestions.json";
    public static final String AUTH_CODE_LOC = "authCode";
    public static final String AUTH_CODE_STATUS_LOC = "statusCode";
    public static final String CHK_CODE_STATUS_LOC = "chkCode";

    public static final int NO_CODE = -1;
    public static final int UKN_CODE = 0;
    public static final int BAD_CODE = 1;
    public static final int GOOD_CODE = 2;
    public static final int CHK_OFF = 0;
    public static final int CHK_ON = 1;

    public static void saveCode(Context curCont, String file, String savedCode){
        FileOutputStream outputStream;

        try {
            outputStream = curCont.openFileOutput(file, Context.MODE_PRIVATE);
            outputStream.write(savedCode.getBytes());
            outputStream.close();
        } catch (Exception e) {
            //TODO Add error message.
            e.printStackTrace();
        }
    }

    public static String getSavedCode(Context curCont, String file) {
        FileInputStream inputStream;
        String results = "";

        try {
            inputStream = curCont.openFileInput(file);
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
            if (file.equals(CHK_CODE_STATUS_LOC)) return String.valueOf(CHK_OFF);
            return "";
        } catch (Exception e){
            //TODO Add error message.
            e.printStackTrace();
        }

        return results.trim();
    }

    public static void generateToast(Context curContext, String toastMsg){
        Context context = curContext.getApplicationContext();
        CharSequence text = toastMsg;
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

}
