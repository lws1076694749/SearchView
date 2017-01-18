package com.lws.searchview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void start(View view) {
        SearchView searchView = (SearchView) findViewById(R.id.search_view);
        searchView.setMaxCount(2);
        searchView.start();
    }

    public void stop(View view) {
        SearchView searchView = (SearchView) findViewById(R.id.search_view);
        searchView.stop();
    }
}
