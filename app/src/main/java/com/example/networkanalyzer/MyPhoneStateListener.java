package com.example.networkanalyzer;

import android.annotation.SuppressLint;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;

class MyPhoneStateListener extends PhoneStateListener {

    int mSignalStrength = 0;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        super.onSignalStrengthsChanged(signalStrength);
        mSignalStrength = signalStrength.getLevel();
        if(MainActivity.mobileConnected) MainActivity.power.setText(mSignalStrength + "/4");
    }

}
