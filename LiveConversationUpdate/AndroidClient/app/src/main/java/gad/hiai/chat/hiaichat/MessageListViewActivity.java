package gad.hiai.chat.hiaichat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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

public class MessageListViewActivity extends AppCompatActivity {
    String replyReceiverUsername;
    MainActivity mainActivity = new MainActivity();
    private Context messageListViewActivityContext;
    private ListView messageListView;
    private int numMessages;
    private Handler updateMessagesHandler;
    private Runnable updateMessagesRunnable;

    ArrayList<String> receivedMessages;
    ArrayList<String> sendersUsernames;
    ArrayList<String> messagesDates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_list_view);

        messageListViewActivityContext = this;

        messageListView = findViewById(R.id.messageListView);

        Intent intent = getIntent();
        final String conversationOwner;

        conversationOwner = intent.getStringExtra("conversationOwner");
        receivedMessages = intent.getStringArrayListExtra("receivedMessages");
        sendersUsernames = intent.getStringArrayListExtra("sendersUsernames");
        messagesDates = intent.getStringArrayListExtra("messagesDates");

        numMessages = receivedMessages.size();

        replyReceiverUsername = conversationOwner;

        ArrayAdapter adapter = new ArrayAdapter(messageListViewActivityContext, R.layout.message_list_item, R.id.messageView, receivedMessages) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView messageView = view.findViewById(R.id.messageView);
                TextView senderView = view.findViewById(R.id.senderView);
                TextView dateView = view.findViewById(R.id.dateView);

                messageView.setText(receivedMessages.get(position));
                senderView.setText(sendersUsernames.get(position));
                dateView.setText(messagesDates.get(position));

                if (conversationOwner.equals(sendersUsernames.get(position))) {
                    view.setBackgroundColor(Color.parseColor("#FFFFFF"));
                } else {
                    view.setBackgroundColor(Color.parseColor("#d8e3eb"));
                }
                return view;
            }
        };
        messageListView.setAdapter(adapter);
    }

    public void setAlarmForNewMessages() {
        Log.d("UPDATED_MSGS", "Running a background thread for checking for new messages between the logged in user and the other user whose username is " + replyReceiverUsername);
        updateMessagesHandler = new Handler();
        new Thread(new MessageListViewActivity.Task()).start();
    }

    class Task implements Runnable {
        @Override
        public void run() {
            updateMessagesHandler.post(new Runnable() {
                @Override
                public void run() {
                    MessageListViewActivity messageListViewActivity = new MessageListViewActivity();
                    messageListViewActivity.getMessages(replyReceiverUsername, messageListView, receivedMessages, sendersUsernames, messagesDates);
                    updateMessagesHandler.postDelayed(this, 1000);
                    updateMessagesRunnable = this;
//                    updateMessagesHandler.removeCallbacks(this); // Cancel the background thread.
                }
            });
        }
    }

    public void getMessages(String senderUsername, ListView messageListView, ArrayList<String> receivedMessages, ArrayList<String> sendersUsernames, ArrayList<String> messagesDates) {
        JSONObject messageContent = new JSONObject();
        try {
//            messageContent.put("subject", "receive_messages");
            messageContent.put("subject", "receive_new_messages");
            messageContent.put("receiver_username", MainActivity.loginUsername);
            messageContent.put("sender_username", senderUsername);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), messageContent.toString());

        Log.d("UPDATED_MSGS", "Creating an HTTP post request to check for new messages between the user and " + senderUsername);
//        postRequest2(MainActivity.postUrl, body, senderUsername, messageListView);
        postRequest3(MainActivity.postUrl, body, senderUsername, messageListView, receivedMessages, sendersUsernames, messagesDates);
    }

    public void postRequest3(String postUrl, RequestBody postBody, final String senderUsername, final ListView messageListView, final ArrayList<String> receivedMessages, final ArrayList<String> sendersUsernames, final ArrayList<String> messagesDates) {
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
                Log.d("UPDATED_MSGS", "The post failed to be delivered : " + e.getMessage());
                e.printStackTrace();

                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("UPDATED_MSGS", "Failed to Connect to Server to fetch updated messages. Please Try Again.");
                        Toast.makeText(messageListViewActivityContext, "Failed to Connect to Server. Please Try Again.", Toast.LENGTH_LONG).show(); // A message indicating that no messages are delivered for the user.
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
                                Log.d("UPDATED_MSGS", "There are no new messages between the user and " + senderUsername + ".");
//                                Toast.makeText(messageListViewActivityContext, "There are no new messages between the user and " + senderUsername, Toast.LENGTH_LONG).show(); // A message indicating that no messages are delivered for the user.
                            }
                        });
                        return;
                    }

//                    final ArrayList<String> receivedMessages = new ArrayList<>();
//                    final ArrayList<String> sendersUsernames = new ArrayList<>();
//                    final ArrayList<String> messagesDates = new ArrayList<>();

                    Log.d("UPDATED_MSGS", "Response from the server as it is : " + responseString);
                    JSONObject messageContent = new JSONObject(responseString);
                    Log.d("UPDATED_MSGS", "Server responded by the messages in the conversation between the user and " + senderUsername + ", " + messageContent);
                    try {
                        for (int i = 0; i < messageContent.length(); i++) {
                            JSONObject currMessage = messageContent.getJSONObject(i + "");
                            String textMessage = currMessage.getString("message");
                            String messageDate = currMessage.getString("date");
                            String messageSenderUsername = currMessage.getString("sender_username");

                            receivedMessages.add(0, textMessage);
                            sendersUsernames.add(0, messageSenderUsername);
                            messagesDates.add(0, messageDate);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    int numUpdatedMessages = receivedMessages.size();
                    if(numMessages == numUpdatedMessages){
                        Log.d("UPDATED_MSGS", "No new messages in the conversation between the user and " + senderUsername);
                        return;
                    }

                    Log.d("UPDATED_MSGS", "There are nNew messages in the conversation between the user and " + senderUsername + " and they are fetched successfully.");

                    Log.d("UPDATED_MSGS", "Updated messages in the conversation between the user and " + senderUsername + " are as follows : " + receivedMessages.toString());

                    final ArrayAdapter adapter = new ArrayAdapter(MainActivity.mainActivityContext, R.layout.message_list_item, R.id.messageView, receivedMessages) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
//                TextView text1 = view.findViewById(android.R.id.text1);
//                TextView text2 = view.findViewById(android.R.id.text2);
                            TextView messageView = view.findViewById(R.id.messageView);
                            TextView senderView = view.findViewById(R.id.senderView);
                            TextView dateView = view.findViewById(R.id.dateView);

                            messageView.setText(receivedMessages.get(position));
                            senderView.setText(sendersUsernames.get(position));
                            dateView.setText(messagesDates.get(position));

                            if (senderUsername.equals(sendersUsernames.get(position))) {
                                view.setBackgroundColor(Color.parseColor("#FFFFFF"));
                            } else {
                                view.setBackgroundColor(Color.parseColor("#d8e3eb"));
                            }
                            return view;
                        }
                    };
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("UPDATED_MSGS", "Updating the ListView Adapter by the new messages in the conversation between the user and " + senderUsername + " are as follows : " + receivedMessages.toString());
                            messageListView.setAdapter(adapter);
                        }
                    });

                } catch (Exception e) {
                    Log.d("UPDATED_MSGS", "Unexpected error happended while fetching the updated messages " + messageListView + " " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

}