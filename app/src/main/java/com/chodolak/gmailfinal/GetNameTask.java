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
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Profile;
import com.google.api.services.gmail.model.Thread;
import org.json.JSONException;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.text.ParseException;

import java.util.Date;

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
        Profile p;
        Thread response;
        List<Message> m = null;
        List<Thread> t = null;
        BigInteger i;
        ArrayList<String> subs = new ArrayList<String>();
        ArrayList<String> body = new ArrayList<String>();

        ArrayList<String> l = new ArrayList<String>();
        StringBuilder builder = new StringBuilder();
        String body2 = "";

        //Note for later.
        //p = service.users().getProfile("me").execute();
        //i = p.getHistoryId();
        //Log.d("Task", "Test history id: " + i);

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
                    body2 = new String(Base64.decodeBase64(test.getPayload().getBody().getData()));
                }
            }
            if(!body.toString().isEmpty()){
                body.add(builder.toString());
            }else{
                body.add(body2);
            }

            for( MessagePartHeader h : messageHeader) {
                if(h.getName().equals("Subject")){
                    l.add(h.getValue());
                    subs.add(h.getValue());
                    mActivity.list(l);
                    break;
                }else if(h.getName().equals("Date")){
                    int emailDate[] = getDate(h.getValue());

                }
            }

        }


        mActivity.list(l);
        mActivity.setItemListener(body, subs);
        mActivity.hideSpinner();

    }

    public int[] getDate(String time){
        int day[] = {0,0,0};
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                "EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
        Date date = null;
        try {
            date = simpleDateFormat.parse(time);

            Calendar fDate = Calendar.getInstance();
            fDate.setTime(date);

            simpleDateFormat = new SimpleDateFormat("dd");
            String d =  simpleDateFormat.format(fDate.getTime());
            day[0] = Integer.parseInt(d);

            simpleDateFormat = new SimpleDateFormat("MM");
            d =  simpleDateFormat.format(fDate.getTime());
            day[1] = Integer.parseInt(d);

            simpleDateFormat = new SimpleDateFormat("yyyy");
            d =  simpleDateFormat.format(fDate.getTime());
            day[2] = Integer.parseInt(d);

            return day;
        } catch (ParseException e) {
            e.printStackTrace();
            return day;
        }
    }

    public int[] getCurrentDate(){
        int day[] = {0,0,0};
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd");
        String dd = sdf.format(c.getTime());
        day[0] = Integer.parseInt(dd);

        sdf = new SimpleDateFormat("MM");
        String mm = sdf.format(c.getTime());
        day[1] = Integer.parseInt(mm);

        sdf = new SimpleDateFormat("yyyy");
        String yy = sdf.format(c.getTime());
        day[2] = Integer.parseInt(yy);

        return day;
    }
}
