package gad.hiai.chat.hiaichat;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NewMessagesBackgroundNotification {
    MainActivity mainActivity = new MainActivity();

    String readTextFromFile(Context context, String fileName) {
        Log.d("ALARM", "Inside readTextFromFile() for reading a text file named " + fileName);
        String returnString = "";

        try {
            InputStream inputStream = context.openFileInput(fileName);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                returnString = stringBuilder.toString();
                Log.d("ALARM", "Data read from the text file named " + fileName + " is : " + returnString);
            }
        } catch (FileNotFoundException e) {
            Log.e("ALARM", "No text file named " + fileName + " is found : " + e.toString());
        } catch (IOException e) {
            Log.e("ALARM", "Can not read the text file named " + fileName + " : " + e.toString());
        }
        return returnString;
    }

    public void checkNewMessages(Context context) {
        String username = readTextFromFile(context, "login_username.txt"); // Save username in a text file in the internal storage.
        String firstName = readTextFromFile(context, "first_name.txt"); // Save first name in a text file in the internal storage.
        String lastName = readTextFromFile(context, "last_name.txt"); // Save last name in a text file in the internal storage.
//        Toast.makeText(context, username, Toast.LENGTH_SHORT).show();

        if (username.equals("") || firstName.equals("") || lastName.equals("")) {
            Log.d("ALARM", "No user is logged in.");
//            Toast.makeText(context, "Please login first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("ALARM", "A user is logged in with first name " + firstName + " and last name " + lastName);

        JSONObject messageContent = new JSONObject();
        try {
            messageContent.put("subject", "check_new_messages");
            messageContent.put("receiver_username", username);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), messageContent.toString());

        Log.d("ALARM", "Checking if there are new messages sent to user " + firstName + " " + lastName);
        postRequest(MainActivity.postUrl, body, context, firstName, lastName);
    }

    public void postRequest(String postUrl, RequestBody postBody, final Context context, final String firstName, final String lastName) {
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
                Log.d("ALARM", "Failed to Connect to Server. The device may not be connected to the Internet : " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, final Response response) {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                try {
                    String responseString = response.body().string();
                    Log.d("ALARM", "Response from the server : " + responseString);
                    JSONObject loginJSONObject = new JSONObject(responseString);
                    String numMessages = loginJSONObject.getString("num_messages");
                    if (numMessages.equals("0")) {
                        Log.d("ALARM", "There are no new messages for the user.");
//                        mainActivity.showNotification(context, "No new messages", "No New Messages");
                        return;
                    }
                    Log.d("ALARM", numMessages + " new message(s) are sent to the user from " + loginJSONObject.getString("senders"));
                    mainActivity.showNotification(context, numMessages + " new message(s) from " + loginJSONObject.getString("senders"), "New Messages");
                } catch (Exception e) {
                    Log.d("ALARM", "Unexpected failure for checking for the new messages. Where are you app/Server developer (Ahmed Gad)?");
                    e.printStackTrace();
                }
            }
        });
    }
}