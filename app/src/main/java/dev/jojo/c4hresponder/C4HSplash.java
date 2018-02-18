package dev.jojo.c4hresponder;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class C4HSplash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_c4_hsplash);

        startActivity(new Intent(getApplicationContext(),RespondLocation.class));


    }
}
