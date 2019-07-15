package com.example.networkanalyzer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    String ipAddress;
    String operator;
    GridLayout mainGrid;
    ImageView imageType;
    ImageView imageOperator;
    TextView download;
    TextView frequency;
    TextView ip;
    TextView ping;
    static TextView power;
    TextView standard;
    Button refresh;
    boolean wifiConnected;
    static boolean mobileConnected;
    TelephonyManager mTelephonyManager;
    MyPhoneStateListener mPhoneStatelistener;
    private static final int REQUEST_OPERATOR = 1;
    final double[] RXOld = new double[1];
    private static String file_url = "http://ipv4.download.thinkbroadband.com:8080/5MB.zip";
    private int size = 0;
    private double speed[] = new double[30];
    private double pom = 0;
    boolean done = true;
    OperatorHolder operatorHolder;


    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainGrid = (GridLayout) findViewById(R.id.mainGrid);
        imageType = (ImageView) findViewById(R.id.imageType);
        imageOperator = (ImageView) findViewById(R.id.imageOperator);
        download = (TextView) findViewById(R.id.Download);
        frequency = (TextView) findViewById(R.id.Upload);
        ip = (TextView) findViewById(R.id.IP);
        ping = (TextView) findViewById(R.id.Ping);
        power = (TextView) findViewById(R.id.Power);
        standard = (TextView) findViewById(R.id.Standard);
        refresh = (Button) findViewById(R.id.refreshButton);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_OPERATOR);
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_OPERATOR);
        }

        refresh.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
            @Override
            public void onClick(View v) {
                refresh();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void refresh() {
        clear();
        checkStatusType();
        checkOperatorName();
        checkIPAddress();
        checkStandard();
        checkSignalStrength();
        downloadSpeed();
        checkSignalFrequency();
        pingGoogle();
    }

    private void pingGoogle() {
        if(wifiConnected || mobileConnected) {
            Runtime runtime = Runtime.getRuntime();
            try {
                Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
                long start = System.currentTimeMillis();
                ipProcess.waitFor();
                long end = System.currentTimeMillis();
                long time = end - start - 10;
                if(time <= 0) ping.setText("-/-");
                else if(time>=100) ping.setText("Time's out");
                else ping.setText(time + " ms");
            } catch (IOException e) {
                e.printStackTrace();
                ping.setText("...");
            } catch (InterruptedException e) {
                e.printStackTrace();
                ping.setText("...");
            }
        }
        else ping.setText("-/-");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void checkSignalFrequency() {
        frequency.setText("-/-");
        if(wifiConnected){
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if(wifiInfo!=null){
                int frequencyRate = wifiInfo.getFrequency();
                if(frequencyRate!=-1) frequency.setText(frequencyRate + " MHz" );
                else frequency.setText("-/-");
            }
        }
        else if(mobileConnected) frequency.setText("Hard to measure");
        else frequency.setText("-/-");
    }

    private void clear() {
        standard.setText(" ");
        power.setText(" ");
        ping.setText(" ");
        ip.setText(" ");
        download.setText(" ");
        frequency.setText(" ");
        imageOperator.setImageResource(0);
        imageType.setImageResource(0);
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStatelistener, PhoneStateListener.LISTEN_NONE);
        mPhoneStatelistener = null;
        operator = null;
        operatorHolder = null;
        wifiConnected = false;
        mobileConnected = false;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private void downloadSpeed() {

        if (mobileConnected && !done) {
            final Handler handler = new Handler();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    double maxDataRate;
                    if (!done){
                        double overallTraffic = TrafficStats.getMobileRxBytes();
                        double currentDataRate = overallTraffic - RXOld[0];
                        currentDataRate = currentDataRate * 8 / (1024 * 1024);
                        speed[size] = currentDataRate;
                        if (speed[size] == 0 && !done) {
                            for (int i = 0; i < size; i++) {
                                for (int j = 0; j < size; j++) {
                                    if (speed[j] > speed[j + 1]) {
                                        pom = speed[j];
                                        speed[j] = speed[j + 1];
                                        speed[j + 1] = pom;
                                    }
                                }
                            }
                            maxDataRate = speed[size - 1];
                            maxDataRate = round(maxDataRate, 1);
                            download.setText(maxDataRate + " Mb/s");
                            done = true;
                    }
                    size++;
                    RXOld[0] = overallTraffic;
                    handler.postDelayed(this, 1000);}
                    else handler.removeCallbacks(this);
                }
            };
            if (!done) {
                handler.postDelayed(runnable, 1000);
                new DownloadFileFromURL().execute(file_url);
            } else handler.removeCallbacks(runnable);
        } else if (wifiConnected && done) {
            final Handler handler = new Handler();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if(done && wifiConnected){
                    if (wifiInfo != null) {
                        Integer linkSpeed = wifiInfo.getLinkSpeed();
                        if(linkSpeed == -1){download.setText("-/-");}
                        else download.setText(linkSpeed + " Mb/s");//measured using WifiInfo.LINK_SPEED_UNITS
                    }
                    handler.postDelayed(this,1000);
                    } else {
                        download.setText("-/-");
                        handler.removeCallbacks(this);}
                }
            };
            handler.postDelayed(runnable, 1000);

        } else {
            final Handler handler = new Handler();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if(done && !wifiConnected) {
                        download.setText("-/-");
                        handler.postDelayed(this, 1000);
                    }
                    else handler.removeCallbacks(this);
                }
            };
            handler.postDelayed(runnable, 1000);
        }
        /* 2 wersja
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {

              //  if() {
                    if (mobileConnected && !wifiConnected) {
                        double maxDataRate;
                            double overallTraffic = TrafficStats.getMobileRxBytes();
                            double currentDataRate = overallTraffic - RXOld[0];
                            currentDataRate = currentDataRate * 8 / (1024 * 1024);
                            speed[size] = currentDataRate;
                            if (speed[size] == 0) {
                                for (int i = 0; i < size; i++) {
                                    for (int j = 0; j < size; j++) {
                                        if (speed[j] > speed[j + 1]) {
                                            pom = speed[j];
                                            speed[j] = speed[j + 1];
                                            speed[j + 1] = pom;
                                        }
                                    }
                                }
                                maxDataRate = speed[size - 1];
                                maxDataRate = round(maxDataRate, 1);
                                download.setText(maxDataRate + " Mb/s");
                                //done = true;
                            }
                            size++;
                            RXOld[0] = overallTraffic;
                    }
                    else if(!wifiConnected && !mobileConnected) {download.setText("-/-");
                    handler.removeCallbacks(this);}
                    else {
                        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        if (done && wifiConnected) {
                            if (wifiInfo != null) {
                                Integer linkSpeed = wifiInfo.getLinkSpeed();
                                if (linkSpeed == -1) {
                                    download.setText("-/-");
                                } else
                                    download.setText(linkSpeed + " Mb/s");//measured using WifiInfo.LINK_SPEED_UNITS
                            }
                        }
                    }
                    handler.postDelayed(this, 1000);
               // }
                //else handler.removeCallbacks(this);
            }
        };
        handler.postDelayed(runnable, 1000);
        if(mobileConnected && !wifiConnected) new DownloadFileFromURL().execute(file_url);
        */
    }

    @SuppressLint("SetTextI18n")
    private void checkSignalStrength() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = manager.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            if (wifiConnected) {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int level = wifiInfo.getRssi();
                power.setText(level + " dBm");
            } else if (mobileConnected) {
                mPhoneStatelistener = new MyPhoneStateListener();
                mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                mTelephonyManager.listen(mPhoneStatelistener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            } else {
                power.setText("-/-");
            }
        } else {
            power.setText("-/-");
        }
    }

    private void checkStandard() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) standard.setText("-/-");
        else if (info.getType() == ConnectivityManager.TYPE_WIFI) standard.setText("WiFi");
        else {
            int networkType = info.getSubtype();
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    standard.setText("1xRTT");
                    break;
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    standard.setText("CDMA");
                    break;
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    standard.setText("EDGE");
                    break;
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                    standard.setText("eHRPD");
                    break;
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    standard.setText("EVDO rev. 0");
                    break;
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    standard.setText("EVDO rev. A");
                    break;
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    standard.setText("EVDO rev. B");
                    break;
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    standard.setText("GPRS");
                    break;
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    standard.setText("HSDPA");
                    break;
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    standard.setText("HSPA");
                    break;
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    standard.setText("HSPA+");
                    break;
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    standard.setText("HSUPA");
                    break;
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    standard.setText("iDen");
                    break;
                case TelephonyManager.NETWORK_TYPE_LTE:
                    standard.setText("LTE");
                    break;
                case 19:
                    standard.setText("LTE CA");
                    break;
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    standard.setText("UMTS");
                    break;
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                    standard.setText("Unknown");
                    break;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    private void checkIPAddress() {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        ipAddress = Formatter.formatIpAddress(manager.getConnectionInfo().getIpAddress());
        ip.setText(ipAddress);
        if (!wifiConnected) {
            try {
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface intf : interfaces) {
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress()) {
                            String sAddr = addr.getHostAddress();
                            boolean isIPv4 = sAddr.indexOf(':') < 0;
                            if (isIPv4) ip.setText(sAddr);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void checkOperatorName() {
        operatorHolder = new OperatorHolder(this);
        operator = operatorHolder.getOperatorName();
        imageOperator.setImageResource(0);
        if (mobileConnected && !done) {
            if (operator.contains("Plus")) imageOperator.setImageResource(R.drawable.plus);
            else if (operator.contains("PLAY")) imageOperator.setImageResource(R.drawable.play);
            else if (operator.contains("Orange")) imageOperator.setImageResource(R.drawable.orange);
            else if (operator.contains("T-Mobile")) imageOperator.setImageResource(R.drawable.tmobilesquare);
            else if (operator.contains("INEA")) imageOperator.setImageResource(R.drawable.inea);
        }else {
            imageOperator.setImageResource(R.drawable.unva);
        }
    }

    private void checkStatusType() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = manager.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            if (wifiConnected) {
                imageType.setImageDrawable(getResources().getDrawable(R.drawable.wifi));
            } else if (mobileConnected) {
                imageType.setImageDrawable(getResources().getDrawable(R.drawable.transfer));
                done = false;
            }
        } else imageType.setImageDrawable(getResources().getDrawable(R.drawable.nocon));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onResume() {
        super.onResume();
        refresh();
    }
}