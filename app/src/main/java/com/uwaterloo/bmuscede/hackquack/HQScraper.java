package com.uwaterloo.bmuscede.hackquack;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.JsonReader;

import com.uwaterloo.bmuscede.solver.CheapDetector;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import tech.gusavila92.websocketclient.WebSocketClient;

/**
 * Created by bmuscede on 09/03/18.
 */

public class HQScraper extends Service{
    private static final String CHANNEL_ID = "hqNotifciation";
    private static final String HTTPS_PROTO = "https";
    private static final String SERVER_IP = "54.85.85.160";
    private static final int SERVER_PORT = 443;
    private static final String ACTIVE_KEY = "active";
    private static final String QUESTION_KEY = "question";
    private static final String ANSWER_KEY = "answers";
    private static final String ANSWER_TEXT_KEY = "text";
    private static final String BROADCAST_KEY = "broadcast";
    private static final String GAME_ID_KEY = "broadcastId";
    private static final String Q_COUNT_KEY = "questionCount";
    private static final String TYPE_KEY = "type";
    private static final String BROADCAST_VALUE = "broadcastEnded";

    private static final int FAST_REFRESH_RATE = 100;
    private static final int SLOW_REFRESH_RATE = 5000;

    private int curNotification = -1;
    private boolean keepLooping = true;
    private String hqAuth;

    protected enum GAME_STATE {INACTIVE, WAITING, PLAY};
    protected GAME_STATE state;

    private int qNum;
    private int qTotal;

    protected Thread hqRun;
    private NotificationManagerCompat answerManager;
    private CheapDetector detector;
    private UICallback startActivity;

    private HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public class LocalBinder extends Binder {
        HQScraper getService() {
            return HQScraper.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Creates a notification manager instance and answer detector.
        answerManager = NotificationManagerCompat.from(this);
        detector = new CheapDetector();

        //Sets up the state.
        state = GAME_STATE.INACTIVE;

        //Creates a handler to run code.
        hqRun = new ScrapeRunnable();
        hqRun.start();

        //Loads in the HQ authorization code.
        loadAuthorizationCode();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy(){
        keepLooping = false;
        super.onDestroy();
    }

    public void registerUICallback(UICallback activity){
        startActivity = activity;
    }

    private void loadAuthorizationCode(){
        FileInputStream inputStream;
        String results = "";

        try {
            inputStream = openFileInput(StartActivity.AUTH_CODE_LOC);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            results = sb.toString();
        } catch (Exception e){
            e.printStackTrace();
        }

        hqAuth = results.trim();
    }

    private void generateHQNotification(Integer titleID, String content){
        //Builds the notification.
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.duck)
                .setContentTitle(getString(titleID))
                .setContentText(content)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationManager.IMPORTANCE_HIGH);

        //Checks whether we should dismiss the last notification.
        curNotification++;
        if (curNotification != 0) answerManager.cancel(curNotification - 1);

        answerManager.notify(curNotification, mBuilder.build());

    }

    private int checkForGame(){
        //First, try to connect to HQ.
        InputStreamReader response;
        try {
            response = connectToHQ("/shows/now/?type=");
        } catch (Exception e){
            //Notifies of the problem.
            generateHQNotification(R.string.notification_error, getString(R.string.conn_eror));
            if (startActivity != null) startActivity.resetHQButton();
            return -1;
        }

        //Converts to JSON.
        JsonReader jsonReader = new JsonReader(response);
        try {
            jsonReader.beginObject();

            while (jsonReader.hasNext()) {
                String key = jsonReader.nextName();
                if (key.equals(ACTIVE_KEY)) {
                    boolean active = jsonReader.nextBoolean();
                    if (active == true) {
                        generateHQNotification(R.string.notification_alert,
                                getString(R.string.game_start));
                        state = GAME_STATE.WAITING;
                    }
                } else if (key.equals(BROADCAST_KEY) && state == GAME_STATE.WAITING) {
                    if (key.equals(BROADCAST_KEY)){
                        jsonReader.beginObject();

                        while (jsonReader.hasNext()){
                            key = jsonReader.nextName();
                            if (key.equals(GAME_ID_KEY)){
                                int gID = jsonReader.nextInt();
                                jsonReader.close();
                                return gID;
                            } else {
                                jsonReader.skipValue();
                            }
                        }
                    }
                } else {
                    jsonReader.skipValue();
                }
            }
        } catch (Exception e){
            return -1;
        }

        return -1;
    }

    private void connectToGameSocket(int gameID) throws Exception{
        URI uri;
        try {
            uri = new URI("wss://ws-quiz.hype.space/ws/" + gameID);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        WebSocketClient webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                //The connection succeeded!
                if (startActivity != null) startActivity.setAuthStatusCode(StartActivity.GOOD_CODE);
            }

            @Override
            public void onTextReceived(String message) {
                //Converts to JSON.
                InputStream stream = new ByteArrayInputStream(
                        message.getBytes(StandardCharsets.UTF_8));
                System.out.println(message);
                JsonReader reader = null;
                try {
                    reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));

                    //Determine the question type.
                    reader.beginObject();
                    while (reader.hasNext()){
                        String key = reader.nextName();
                        System.out.println(key);
                        if (key.equals(TYPE_KEY)){
                            //Determine if we have a question.
                            String value = reader.nextString();
                            if (value.equals(QUESTION_KEY)){
                                qNum++;
                                answerQuestion(reader);
                                if (qTotal == 0) qTotal = getQCount(reader);
                                return;
                            } else if (value == BROADCAST_VALUE){
                                //Reconnect.

                            } else {
                                reader.close();
                                return;
                            }
                        } else {
                            reader.skipValue();
                        }
                    }

                } catch (Exception e){ return; }
            }

            @Override
            public void onBinaryReceived(byte[] data) { }

            @Override
            public void onPingReceived(byte[] data) { }

            @Override
            public void onPongReceived(byte[] data) { }

            @Override
            public void onException(Exception e) {
                state = GAME_STATE.INACTIVE;
                generateHQNotification(R.string.notification_error, getString(R.string.conn_eror));
                if (startActivity != null) {
                    startActivity.setAuthStatusCode(StartActivity.BAD_CODE);
                    startActivity.resetHQButton();
                }
                System.out.println(e.getMessage());
            }

            @Override
            public void onCloseReceived() {
                if (qNum == qTotal && qTotal != 0){
                    state = GAME_STATE.INACTIVE;
                    qTotal = 0;
                    qNum = 0;

                    //Tell the user that the game is over.
                    generateHQNotification(R.string.notification_alert,
                            "The game has ended! Goodbye!");
                } else {
                    state = GAME_STATE.WAITING;
                }
            }
        };

        webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(60000);
        webSocketClient.addHeader("Authorization", hqAuth);
        webSocketClient.enableAutomaticReconnection(0);
        webSocketClient.connect();

    }

    private void answerQuestion(JsonReader reader) {
        //Gets the questions and answers.
        String question = "";
        List<String> answers = new ArrayList<String>();

        try {
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (key.equals(QUESTION_KEY)){
                    question = reader.nextString();
                } else if (key.equals(ANSWER_KEY)) {
                    reader.beginArray();

                    while (reader.hasNext()){
                        reader.beginObject();
                        while (reader.hasNext()){
                            key = reader.nextName();
                            if (key.equals(ANSWER_TEXT_KEY)){
                                answers.add(reader.nextString());
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();;
                    }

                    reader.endArray();
                } else  {
                    reader.skipValue();
                }
            }
        } catch (Exception e) {
            generateHQNotification(R.string.notification_error, getString(R.string.quest_error));
            return;
        }

        //Answers the questions.
        int answerVal = detector.determineAnswer(question, answers);
        double answerConf = detector.getConfidence();

        generateHQNotification(R.string.notification_title, "The answer is " +
                answers.get(answerVal) + " (Accuracy: " + answerConf + "%)!");
    }

    private int getQCount(JsonReader reader){
        int total = 0;

        try {
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (key.equals(Q_COUNT_KEY)){
                    total = reader.nextInt();
                } else {
                    reader.skipValue();
                }
            }
        } catch (Exception e) {
            return 0;
        }

        return total;
    }

    private InputStreamReader connectToHQ(String getReq) throws Exception {
        //Connects to HQ's REST API
        //URL hqEndpoint = new URL(HQ_URL + getReq);
        URL hqEndpoint = new URL(HTTPS_PROTO, SERVER_IP, SERVER_PORT, getReq);
        HttpsURLConnection hqConnection =
                (HttpsURLConnection) hqEndpoint.openConnection();

        //Sets a bunch of important variables to gain access.
        hqConnection.setHostnameVerifier(hostnameVerifier);
        hqConnection.setRequestProperty("x-hq-client", "Android/1.4.0");
        hqConnection.setRequestProperty("x-hq-country", "CA");
        hqConnection.setRequestProperty("x-hq-lang", "en");
        hqConnection.setRequestProperty("Authorization", hqAuth);
        hqConnection.setRequestProperty("x-hq-stk", "MQ==");
        hqConnection.setRequestProperty("Host", "api-quiz.hype.space");
        hqConnection.setRequestProperty("Connection", "Keep-Alive");
        hqConnection.setRequestProperty("Accept-Encoding", "gzip");
        hqConnection.setRequestProperty("User-Agent", "HackQuack/1.0.0");

        //If we get a BAD response, throw an exception.
        if (hqConnection.getResponseCode() != 200){
            throw new Exception("HQ returned a non-200 error code!");
        }

        //Determines whether we read based on GZIP.
        InputStreamReader reader;
        if ("gzip".equals(hqConnection.getContentEncoding())) {
            reader = new InputStreamReader(
                    new GZIPInputStream(hqConnection.getInputStream()), "utf-8");
        } else {
            reader = new InputStreamReader(hqConnection.getInputStream(), "utf-8");
        }

        return reader;
    }

    private class ScrapeRunnable extends Thread {
        @Override
        public void run(){
            int gameID = -1;
            qNum = 0;
            qTotal = 0;
            int refreshRate = 0;

            while (keepLooping){
                if (state == GAME_STATE.INACTIVE){
                    gameID = checkForGame();
                    if (gameID != -1){
                        refreshRate = FAST_REFRESH_RATE;
                    } else {
                        refreshRate = SLOW_REFRESH_RATE;
                    }
                }
                if (state == GAME_STATE.WAITING && gameID != -1){
                    try {
                        connectToGameSocket(gameID);
                        state = GAME_STATE.PLAY;
                        startActivity.setAuthStatusCode(StartActivity.GOOD_CODE);
                    } catch (Exception e){
                        e.printStackTrace();
                        startActivity.setAuthStatusCode(StartActivity.BAD_CODE);
                        startActivity.resetHQButton();
                    }
                }

                SystemClock.sleep(refreshRate);
            }
        }
    }
}
