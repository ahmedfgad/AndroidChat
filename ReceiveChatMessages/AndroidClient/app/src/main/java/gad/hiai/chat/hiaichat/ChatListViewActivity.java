package gad.hiai.chat.hiaichat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

public class ChatListViewActivity extends AppCompatActivity {
    Intent intent;
    MainActivity mainActivity = new MainActivity();
    private Context chatListViewActivityContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list_view);

        chatListViewActivityContext = this;

        ListView messageListView = findViewById(R.id.chatListView);

        intent = getIntent();
        ArrayList<String> receivedChats = intent.getStringArrayListExtra("receivedChats");

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, receivedChats);
        messageListView.setAdapter(adapter);

        messageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView itemTextView = (TextView) view;
                String senderUsername = itemTextView.getText().toString().trim();
//                Toast.makeText(getApplicationContext(), "Sender username " + senderUsername, Toast.LENGTH_SHORT).show();

                JSONObject messageContent = new JSONObject();
                try {
                    messageContent.put("subject", "receive_messages");
                    messageContent.put("receiver_username", MainActivity.loginUsername);
                    messageContent.put("sender_username", senderUsername);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), messageContent.toString());

                postRequest(MainActivity.postUrl, body, senderUsername);
            }
        });
    }
    public void postRequest(String postUrl, RequestBody postBody, final String senderUsername) {
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
                        Toast.makeText(getApplicationContext(), "Failed to Connect to Server. Please Try Again.", Toast.LENGTH_LONG).show(); // A message indicating that no messages are delivered for the user.
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                try {
                    String responseString = response.body().string().trim();

                    if (responseString.equals("0")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Something went wrong.", Toast.LENGTH_LONG).show(); // A message indicating that no messages are delivered for the user.
                            }
                        });
                        return;
                    }

                    Intent showMessagesIntent = new Intent(getApplicationContext(), MessageListViewActivity.class);
                    ArrayList<String> receivedMessages = new ArrayList<>();
                    ArrayList<String> sendersUsernames = new ArrayList<>();
                    ArrayList<String> receiveDates = new ArrayList<>();

                    JSONObject messageContent = new JSONObject(responseString);
                    Log.d("CHATS", "Response from the server : " + messageContent);
                    try {
                        for (int i = 0; i < messageContent.length(); i++) {
                            JSONObject currMessage = messageContent.getJSONObject(i + "");
                            String textMessage = currMessage.getString("message");
                            String messageDate = currMessage.getString("date");
                            String messageSenderUsername = currMessage.getString("sender_username");

                            receivedMessages.add(textMessage);
                            sendersUsernames.add(messageSenderUsername);
                            receiveDates.add(messageDate);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Log.d("MSGS", receivedMessages.toString());
                    showMessagesIntent.putExtra("conversationOwner", senderUsername);
                    showMessagesIntent.putExtra("receivedMessages", receivedMessages);
                    showMessagesIntent.putExtra("sendersUsernames", sendersUsernames);
                    showMessagesIntent.putExtra("messagesDates", receiveDates);
//                    mainActivity.showNotification(chatListViewActivityContext, "You can see messages in the conversation", "Messages Fetched");
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getApplicationContext(), "Messages Fetched and Displayed.", Toast.LENGTH_LONG).show();
//                        }
//                    });
                    startActivity(showMessagesIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}