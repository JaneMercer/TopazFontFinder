package com.example.dezzer.topazfontfinderint;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class EditImage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image);


        Intent intent = getIntent();
    /*    String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        TextView textView = new TextView(this);
        textView.setTextSize(40);
        textView.setText(message);

        ViewGroup layout = (ViewGroup) findViewById(R.id.activity_act__b);
        layout.addView(textView);
 */
    }
    public void cancel_btn ( View view) {
        //   if(context instanceof Activity){
        //     ((Activity)context).finish(); }
        finish();

    }

    public void go_to_actv ( View view) {
        Intent intent = new Intent(this, characterList.class);
        startActivity(intent);
    }

}
