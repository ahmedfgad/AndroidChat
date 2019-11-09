package gad.hiai.chat.hiaichat;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

public class RegisterActivity extends AppCompatActivity {

    private static boolean registerPost = false;
    private static boolean verifyPost = false;
    private Context registerActivityContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        registerActivityContext = this;
    }

    public void register(View v) {
        EditText emailView = findViewById(R.id.email);
        EditText usernameView = findViewById(R.id.username);
        EditText firstNameView = findViewById(R.id.firstName);
        EditText lastNameView = findViewById(R.id.lastName);

        String email = emailView.getText().toString().trim();
        String username = usernameView.getText().toString().trim();
        String firstName = firstNameView.getText().toString().trim();
        String lastName = lastNameView.getText().toString().trim();

        if (email.length() == 0 || username.length() == 0) {
            Toast.makeText(getApplicationContext(), "Something is wrong. Please check your inputs.", Toast.LENGTH_LONG).show();
        } else {
            JSONObject registrationForm = new JSONObject();
            try {
                registrationForm.put("subject", "register");
                registrationForm.put("email", email);
                registrationForm.put("first_name", firstName);
                registrationForm.put("last_name", lastName);
                registrationForm.put("username", username);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), registrationForm.toString());

            registerPost = true;
            postRequest(MainActivity.postUrl, body);
        }
    }

    public void verifyEmail(View v) {
        EditText verificationCodeView = findViewById(R.id.verificationCode);

        EditText firstNameView = findViewById(R.id.firstName);
        EditText lastNameView = findViewById(R.id.lastName);
        EditText emailView = findViewById(R.id.email);
        EditText usernameView = findViewById(R.id.username);
        EditText passwordView = findViewById(R.id.password);

        String verificationCode = verificationCodeView.getText().toString().trim();

        String firstName = firstNameView.getText().toString().trim();
        String lastName = lastNameView.getText().toString().trim();
        String email = emailView.getText().toString().trim();
        String username = usernameView.getText().toString().trim();
        String password = passwordView.getText().toString().trim();

        if (firstName.length() == 0 || lastName.length() == 0 || email.length() == 0 || username.length() == 0 || password.length() == 0) {
            Toast.makeText(getApplicationContext(), "Something is wrong. Please check your inputs.", Toast.LENGTH_LONG).show();
        } else {
            JSONObject registrationForm = new JSONObject();
            try {
                registrationForm.put("subject", "verify");
                registrationForm.put("verification_code", verificationCode);

                registrationForm.put("firstname", firstName);
                registrationForm.put("lastname", lastName);
                registrationForm.put("email", email);
                registrationForm.put("username", username);
                registrationForm.put("password", password);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), registrationForm.toString());

            verifyPost = true;
            postRequest(MainActivity.postUrl, body);
        }
    }

    public void postRequest(String postUrl, RequestBody postBody) {
        OkHttpClient client = new OkHttpClient();

        final Request request = new Request.Builder()
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
                        TextView responseText = findViewById(R.id.responseTextRegister);
                        responseText.setText("Failed to Connect to Server. Please Try Again.");
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                final TextView responseTextRegister = findViewById(R.id.responseTextRegister);
                try {
                    final String responseString = response.body().string().trim();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            EditText firstNameView = findViewById(R.id.firstName);
                            EditText lastNameView = findViewById(R.id.lastName);
                            EditText emailView = findViewById(R.id.email);
                            EditText usernameView = findViewById(R.id.username);
                            EditText passwordView = findViewById(R.id.password);
                            EditText verificationCode = findViewById(R.id.verificationCode);
                            Button verifyButton = findViewById(R.id.verifyButton);
                            Button registerButton = findViewById(R.id.registerButton);
                            if (registerPost == true) { // Response to a registration post
                                if (responseString.equals("success")) {
                                    Toast.makeText(getApplicationContext(), "Pending E-mail Verification", Toast.LENGTH_LONG).show();

                                    firstNameView.setEnabled(false);
                                    lastNameView.setEnabled(false);
                                    emailView.setEnabled(false);
                                    usernameView.setEnabled(false);
                                    passwordView.setEnabled(false);
                                    registerButton.setEnabled(false);

                                    verificationCode.setEnabled(true);
                                    verifyButton.setEnabled(true);
                                    Toast.makeText(registerActivityContext, "Registration is pending e-mail confirmation", Toast.LENGTH_LONG).show();
                                    responseTextRegister.setText("Registration is pending e-mail confirmation.\nGo to your e-mail (check the spam folder), copy the conformation code, paste it down here, and click Confirm.\nYou will not be registred if e-mail is not confirmed.");
                                } else if (responseString.equals("email")) {
                                    responseTextRegister.setText("E-mail is already registered. Please use another e-mail.");
                                } else if (responseString.equals("username")) {
                                    responseTextRegister.setText("Username already exists. Please chose another username.");
                                } else {
                                    responseTextRegister.setText("Something went wrong. Please try again later.");
                                }
                                registerPost = false;
                            } else if (verifyPost == true) { // Response to a verification post
                                if (responseString.equals("success")) {
                                    verificationCode.setEnabled(false);
                                    verifyButton.setEnabled(false);

                                    Toast.makeText(registerActivityContext, "Your e-mail is verified successfully", Toast.LENGTH_LONG).show();
                                    responseTextRegister.setText("Your e-mail is verified successfully and you are now a user. You can login.");

                                    Intent intent = new Intent();
                                    intent.putExtra("status", "Your e-mail is verified successfully and you are now a user. You can login.");
                                    setResult(1, intent);
                                    finish();//finishing activity and return to the calling activity.
                                } else if (responseString.equals("failure")) {
                                    Toast.makeText(registerActivityContext, "You can register again after validating your data", Toast.LENGTH_LONG).show();
                                    responseTextRegister.setText("Sorry. E-mail is not verified. Registration failed. You can register again after validating your data.");
                                } else {
                                    responseTextRegister.setText("Something went wrong. Please try again later.");
                                }
                                verifyPost = false;
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
