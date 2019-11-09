package gad.hiai.chat.hiaichat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;

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

    }
}