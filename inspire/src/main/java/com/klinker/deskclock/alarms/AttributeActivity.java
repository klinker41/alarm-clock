package com.klinker.deskclock.alarms;

import android.os.Bundle;

public class AttributeActivity extends AlarmActivity {

    @Override
    public void onCreate(Bundle savedInstantceState) {
        attributeActivity = true;
        super.onCreate(savedInstantceState);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
