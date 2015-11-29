package com.csci4100.fab;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class LoadingScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_screen);

        findViewById(R.id.avloadingIndicatorView).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        findViewById(R.id.avloadingIndicatorView).setVisibility(View.GONE);
        super.onDestroy();
    }

}