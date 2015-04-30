package com.chodolak.gmailfinal;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Thread;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**

 import com.google.android.gms.auth.GoogleAuthUtil;

 import android.os.AsyncTask;
 import android.util.Log;

 import org.json.JSONException;
 import org.json.JSONObject;

 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.HttpURLConnection;
 import java.net.URL;

 /**
 * Display personalized greeting. This class contains boilerplate code to consume the token but
 * isn't integral to getting the tokens.
 */
public class GetNameTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "TokenInfoTask";
    private static final String NAME_KEY = "given_name";
    protected MainActivity mActivity;

    protected String mScope;
    protected String mEmail;

    GetNameTask(MainActivity activity, String email, String scope) {
        this.mActivity = activity;
        this.mScope = scope;
        this.mEmail = email;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            fetchNameFromProfileServer();
        } catch (IOException ex) {
            onError("Following Error occured, please try again. " + ex.getMessage(), ex);
        } catch (JSONException e) {
            onError("Bad response: " + e.getMessage(), e);
        }
        return null;
    }

    protected void onError(String msg, Exception e) {
        if (e != null) {
            Log.e(TAG, "Exception: ", e);
        }
        mActivity.show(msg);  // will be run in UI thread
    }

    /**
     * Get a authentication token if one is not available. If the error is not recoverable then
     * it displays the error message on parent activity.
     */
    protected String fetchToken() throws IOException {
        try {
            return GoogleAuthUtil.getToken(mActivity, mEmail, mScope);
        } catch (UserRecoverableAuthException userRecoverableException) {
            // GooglePlayServices.apk is either old, disabled, or not present, which is
            // recoverable, so we need to show the user some UI through the activity.
            mActivity.handleException(userRecoverableException);
        } catch (GoogleAuthException fatalException) {
            onError("Unrecoverable error " + fatalException.getMessage(), fatalException);
        }
        return null;
    }

    /**
     * Contacts the user info server to get the profile of the user and extracts the first name
     * of the user from the profile. In order to authenticate with the user info server the method
     * first fetches an access token from Google Play services.
     * @throws IOException if communication with user info server failed.
     * @throws JSONException if the response from the server could not be parsed.
     */
    private void fetchNameFromProfileServer() throws IOException, JSONException {
        mActivity.showSpinner();
        mActivity.show("Getting emails...");
        String token = fetchToken();
        if (token == null) {
            return;
        }

        GoogleCredential credential = new GoogleCredential().setAccessToken(token);
        JsonFactory jsonFactory = new JacksonFactory();
        HttpTransport httpTransport = new NetHttpTransport();

        Gmail service = new Gmail.Builder(httpTransport, jsonFactory, credential).setApplicationName("GmailApiTP").build();

        ListThreadsResponse threadsResponse;
        Thread response;
        List<Message> m = null;
        List<Thread> t = null;
        ArrayList<String> subs = new ArrayList<String>();
        ArrayList<String> body = new ArrayList<String>();
        ArrayList<String> l = new ArrayList<String>();
        StringBuilder builder = new StringBuilder();
        int testingValue = 0;
        String testingString = "";
        try {
            threadsResponse = service.users().threads().list("me").execute();
            t = threadsResponse.getThreads();
        } catch (IOException e) {
            e.printStackTrace();
        }



        for(Thread thread : t) {
            String id = thread.getId();
            response = service.users().threads().get("me",id).execute();
            List<MessagePartHeader> messageHeader = response.getMessages().get(0).getPayload().getHeaders();

            List<Message> testing = response.getMessages();
            for(Message test : testing){
                if(test.getPayload().getMimeType().contains("multipart")){
                    builder = new StringBuilder();
                    for(MessagePart part : test.getPayload().getParts()){
                        if (part.getMimeType().contains("multipart")) {
                            for (MessagePart part2 : part.getParts()) {
                                if (part2.getMimeType().equals("text/plain")) {
                                    builder.append(new String(
                                            Base64.decodeBase64(part2.getBody().getData())));
                                }
                            }
                        }else if (part.getMimeType().equals("text/plain")) {
                            builder.append(new String(Base64.decodeBase64(part.getBody().getData())));
                        }
                    }
                }else{
                    String body2 = new String(Base64.decodeBase64(test.getPayload().getBody().getData()));
                }
            }
            if(testingValue == 0){
                testingString = builder.toString();
                testingValue++;
            }

            for( MessagePartHeader h : messageHeader) {
                if(h.getName().equals("Subject")){
                    l.add(h.getValue());
                    subs.add(h.getValue());
                    mActivity.list(l);
                    break;
                }
            }



        }
        mActivity.list(l);
        mActivity.setItemListener(testingString);
        mActivity.hideSpinner();

    }

    /**
     * Reads the response from the input stream and returns it as a string.
     */
    private static String readResponse(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] data = new byte[2048];
        int len = 0;
        while ((len = is.read(data, 0, data.length)) >= 0) {
            bos.write(data, 0, len);
        }
        return new String(bos.toByteArray(), "UTF-8");
    }

    /**
     * Parses the response and returns the first name of the user.
     * @throws JSONException if the response is not JSON or if first name does not exist in response
     */
    private String getFirstName(String jsonResponse) throws JSONException {
        JSONObject profile = new JSONObject(jsonResponse);
        return profile.getString(NAME_KEY);
    }
}
