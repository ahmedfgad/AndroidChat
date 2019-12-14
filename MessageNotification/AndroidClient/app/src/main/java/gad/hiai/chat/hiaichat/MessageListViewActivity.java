package gad.hiai.chat.hiaichat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;

public class MessageListViewActivity extends AppCompatActivity {
    String replyReceiverUsername;
    MainActivity mainActivity = new MainActivity();
    private Context messageListViewActivityContext;
    private ListView messageListView;
    private int numMessages;

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
}