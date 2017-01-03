package com.codefromlab.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.codefromlab.profilestackview.StackView;

public class MainActivity extends AppCompatActivity {

    StackView stackView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stackView = (StackView) findViewById(R.id.stack_view);

    }

    public void showItem(View view) {
        stackView.setNewItem();
    }
}
