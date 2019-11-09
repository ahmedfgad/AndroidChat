package gad.hiai.chat.hiaichat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SendMessageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
    }

    public void sendMessage(View v) {
        Button sendMessageButton = findViewById(R.id.sendMessageButton);
        sendMessageButton.setEnabled(false);

        EditText textMessageView = findViewById(R.id.textMsg);
        TextView responseTextSend = findViewById(R.id.responseTextSend);

        if (MainActivity.loginUsername.equals("")) {
            responseTextSend.setText("Please login first.");
            sendMessageButton.setEnabled(true);
            return;
        }

        EditText receiverUserNameView = findViewById(R.id.receiverUserName);
        String receiverUserName = receiverUserNameView.getText().toString();
        if (receiverUserName.length() == 0) {
            responseTextSend.setText("Receiver username is missing.");
            sendMessageButton.setEnabled(true);
            return;
        }

        String textMessage = textMessageView.getText().toString();
        textMessage = textMessage.trim();

        if (textMessage.length() == 0) {
            responseTextSend.setText("Message is Empty. Nothing to Send.");
            sendMessageButton.setEnabled(true);
            return;
        }

        JSONObject messageContent = new JSONObject();
        try {
            messageContent.put("subject", "send");
            messageContent.put("msg", textMessage);
            messageContent.put("sender_username", MainActivity.loginUsername);
            messageContent.put("receiver_username", receiverUserName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        responseTextSend.setText("Sending the message ...");

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), messageContent.toString());

        postRequest(MainActivity.postUrl, body);
    }

    void postRequest(String postUrl, RequestBody postBody) {
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
                        TextView responseTextSend = findViewById(R.id.responseTextSend);
                        responseTextSend.setText("Failed to Connect to Server. Please Try Again.");
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                final Button sendMessageButton = findViewById(R.id.sendMessageButton);
                TextView responseTextSend = findViewById(R.id.responseTextSend);
                EditText textMessageView = findViewById(R.id.textMsg);
                try {
                    String responseString = response.body().string().trim();
                    if (responseString.equals("success")) {
                        responseTextSend.setText("Message Sent Successfully.");

                        textMessageView.setText("");
                    } else {
                        responseTextSend.setText("Failed to send the message. Please try again.\nHint: Make sure username is correct.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sendMessageButton.setEnabled(true);
                    }
                });
            }
        });
    }
}