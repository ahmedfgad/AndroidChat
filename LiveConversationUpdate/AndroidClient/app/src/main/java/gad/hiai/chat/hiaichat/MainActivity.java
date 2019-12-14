package gad.hiai.chat.hiaichat;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    public static Context mainActivityContext;

    public static String loginUsername = "";

    private Handler handler;

    static String postUrl = "http://192.168.16.101:5000/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.INTERNET}, 0);

        mainActivityContext = this;

        setAlarmForNewMessages();
    }

    public void setAlarmForNewMessages() {
        Log.d("ALARM", "Alarm is activated to run in the background for checking for new messages.");
        handler = new Handler();
        new Thread(new Task()).start();
    }

    class Task implements Runnable {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    NewMessagesBackgroundNotification newMessagesBackgroundNotification = new NewMessagesBackgroundNotification();
                    newMessagesBackgroundNotification.checkNewMessages(mainActivityContext);
                    handler.postDelayed(this, 1000);
//                    handler.removeCallbacks(this); // Cancel the background alarm.
                }
            });
        }
    }

    public void login(View v) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, 2);
    }

    public void register(View v) {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    public void sendMessage(View v) {
        TextView responseText = findViewById(R.id.responseText);
        if (loginUsername.equals("")) {
            responseText.setText("Please login first.");
            return;
        }

        Intent intent = new Intent(this, SendMessageActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        TextView responseText = findViewById(R.id.responseText);
        if (requestCode == 2) { // Login
            responseText.setText("Successful Login.");
        } else {
            responseText.setText("Invalid or no data entered. Please try again.");
        }
    }

    public void receiveChat(View v) {
        TextView responseText = findViewById(R.id.responseText);
        if (loginUsername.equals("")) {
            responseText.setText("Please login first.");
            return;
        }

        responseText.setText("Fetching conversations. Please wait ...");

        JSONObject messageContent = new JSONObject();
        try {
            messageContent.put("subject", "receive_chats");
            messageContent.put("receiver_username", loginUsername);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), messageContent.toString());

        postRequest(MainActivity.postUrl, body);
    }

    public void postRequest(String postUrl, RequestBody postBody) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(postUrl)
                .post(postBody)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Cancel the post on failure.
                call.cancel();
                Log.d("FAIL", e.getMessage());

                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView responseText = findViewById(R.id.responseText);
                        responseText.setText("Failed to Connect to Server. Please Try Again.");
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                TextView responseText = findViewById(R.id.responseText);
                try {
                    String responseString = response.body().string().trim();
                    if (responseString.equals("0")) {
                        responseText.setText("No conversations."); // A message indicating that no messages are delivered for the user.
                        return;
                    }

                    Intent showChatsIntent = new Intent(getApplicationContext(), ChatListViewActivity.class);
                    ArrayList<String> sendersUsernames = new ArrayList<>();

                    JSONObject messageContent = new JSONObject(responseString);
                    try {
                        for (int i = 0; i < messageContent.length(); i++) {
                            JSONObject currMessage = messageContent.getJSONObject(i + "");
                            String senderUsername = currMessage.getString("username");
                            sendersUsernames.add(senderUsername);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Log.d("CHATS", sendersUsernames.toString());
                    showChatsIntent.putExtra("receivedChats", sendersUsernames);
                    responseText.setText("Conversations Fetched.");
                    startActivity(showChatsIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void showNotification(final Context context, String message, String messageTitle) {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), 0);
        String channelId = "MessageChannel";
        CharSequence channelName = "New Message";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel(channelId,
                    channelName,
                    importance);
//            notificationChannel.enableLights(true);
//            notificationChannel.setLightColor(Color.RED);
//            notificationChannel.enableVibration(true);
//            notificationChannel.setVibrationPattern(new long[]{1000, 2000});

            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

            notificationManager.createNotificationChannel(notificationChannel);

            final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId);

            notificationBuilder.setContentTitle(messageTitle);
            notificationBuilder.setContentText(message);
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
            notificationBuilder.setChannelId(channelId);
            notificationBuilder.setAutoCancel(true);
            notificationBuilder.setContentIntent(contentIntent);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notificationManager.notify(1, notificationBuilder.build());
                }
            });
//            notificationManager.notify(1, notificationBuilder.build());
        } else {
            final Notification.Builder notificationBuilder = new Notification.Builder(context);

            notificationBuilder.setContentTitle(messageTitle);
            notificationBuilder.setContentText(message);
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
            notificationBuilder.setAutoCancel(true);
            notificationBuilder.setContentIntent(contentIntent);

            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notificationManager.notify(1, notificationBuilder.build());
                }
            });
        }
    }
}