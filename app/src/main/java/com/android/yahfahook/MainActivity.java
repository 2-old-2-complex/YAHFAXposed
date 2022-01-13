package com.android.yahfahook;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends Activity {


    Button buttonTest=null;
    TextView textView=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonTest=(Button)findViewById(R.id.buttonTest);
        textView=(TextView)findViewById(R.id.textViewShowMsg);
        buttonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textView.setText(getMsg());
            }
        });
    }
    private String getMsg(){
        return "001";
    }
}