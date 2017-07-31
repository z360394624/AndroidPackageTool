package com.tec.lucius.demo;

import android.content.Context;
import android.print.PrintManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
    }
}
