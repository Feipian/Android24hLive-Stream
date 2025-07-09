package com.example.android24hstream;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class StreamActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream); // Make sure this layout exists

        // Initialize your streaming components here
        initializeStreaming();
    }

    private void initializeStreaming() {
        // Your streaming initialization code
        // Example:
        // genericFromFile = new GenericFromFile(null, this, this, this);
        // genericFromFile.setLoopMode(true);
    }
}