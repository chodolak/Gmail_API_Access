package com.chodolak.gmailfinal;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by User on 4/29/2015.
 */
public class InfoActivity extends Activity {

    TextView header;
    TextView body;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        header = (TextView)findViewById(R.id.header);
        body = (TextView)findViewById(R.id.body);
        header.setText("This works!");
        String value = "";
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
             value = extras.getString("body");
        }

        body.setText(value);
    }
}
