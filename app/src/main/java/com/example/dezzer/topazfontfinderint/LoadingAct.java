package com.example.dezzer.topazfontfinderint;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class LoadingAct extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

    }

    public void go_to_actv ( View view) {
        Intent intent = new Intent(this, ShowResults.class);
        startActivity(intent);
    }
}
