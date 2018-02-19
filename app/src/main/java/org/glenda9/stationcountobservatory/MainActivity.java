package org.glenda9.stationcountobservatory;

import android.content.pm.PackageManager;
import android.content.Intent;
import android.os.Build;
import android.os.Parcel;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {
    public static final String LOGNAME="scobservatory";
    public static final int IE_ID_CC1X=133;
    public static final String SR_MEMBER_IES="informationElements";
    public static final String SR_IE_MEMBER_ID="id";
    public static final String SR_IE_MEMBER_BYTES="bytes";

    /* menu id */
    public static final int MENU_CC1X_DEVICENAME=10;
    public static final int MENU_QBSS_LOAD=20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(LOGNAME, "starting");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_QBSS_LOAD, Menu.NONE, "QBSS Load");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_QBSS_LOAD:
                Intent intent = new Intent(MainActivity.this, QBSSLoad.class);
                startActivity(intent);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    public void clearCountAndList() {
        List<ScanInfo> emptyDisplayList = new ArrayList<>();
        /* update station count */
        TextView tv = (TextView) findViewById(R.id.station_count);
        tv.setText(String.valueOf(0) + " ");

        ListView lv = (ListView) findViewById(R.id.scan_listview);
        ScanInfoAdapter adapter = new ScanInfoAdapter(this, R.layout.scanlist_item, emptyDisplayList);
        lv.setAdapter(adapter);
    }

    public List<ScanResult> scanNeighbourAP() {
        Log.i(LOGNAME, "start scanning");

        /* Scan requires COARSE_LOCATION permission: grant it here */
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    1001);
        }

        WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
        if (manager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            showToast("Wi-Fi not enabled!");
            return null;
        }

        manager.startScan();
        return manager.getScanResults();
    }

    public ScanInfo parseScanResultToScanInfoByReflection(ScanResult sr) throws NoSuchFieldException, IllegalAccessException {
        ScanInfo sInfo = null;
        int station_count = 0;
        String device_name = "";
        boolean found_count = false;

        Field field;
        Object[] ieArray;

        /* acquire informationElements[] */
        field = sr.getClass().getDeclaredField(SR_MEMBER_IES);
        field.setAccessible(true);
        ieArray = (Object[])field.get(sr);

        for (int i = 0; i < ieArray.length; i++) {
            Object obj = ieArray[i];
            int id;
            byte[] bytes;

            /* acquire IE id */
            field = obj.getClass().getDeclaredField(SR_IE_MEMBER_ID);
            field.setAccessible(true);
            id = (int)field.get(obj);

            /* acquire IE bytes */
            field = obj.getClass().getDeclaredField(SR_IE_MEMBER_BYTES);
            field.setAccessible(true);
            bytes = (byte[])field.get(obj);

            switch (id) {
                case IE_ID_CC1X:
                    int unknown1_idx = 0;
                    int device_name_idx = unknown1_idx + 10;
                    int station_count_idx = device_name_idx + 16;
                    int device_name_end_idx = station_count_idx;


                    for (int idx = device_name_idx; idx < station_count_idx; idx ++) {
                        if (bytes[idx] != 0) {
                            continue;
                        }
                        device_name_end_idx = idx;
                        break;
                    }

                    byte[] device_name_bytes = Arrays.copyOfRange(bytes, device_name_idx, device_name_end_idx);
                    station_count = (int)bytes[station_count_idx];
                    device_name = new String(device_name_bytes);

                    found_count = true;
                    break;
                default:
                    /* ignore */
                    break;
            }
        }

        if (!found_count)
            return null;

        sInfo = new ScanInfo(sr.frequency, sr.SSID, sr.BSSID, device_name, station_count);
        return sInfo;
    }

    public HashMap<ScanInfo, Integer> parseScanResults(List<ScanResult> apList) throws NoSuchFieldException, IllegalAccessException {
        HashMap<ScanInfo, Integer> station_count_map = new HashMap<ScanInfo, Integer>();

        if (apList == null) {
            return null;
        }

        for (int i = 0; i < apList.size(); i++) {
            ScanResult sr = apList.get(i);
            ScanInfo sInfo = parseScanResultToScanInfoByReflection(sr);
            if (sInfo == null) {
                continue;
            }

            Set<ScanInfo> keySet = station_count_map.keySet();
            Iterator<ScanInfo> iterator = keySet.iterator();
            boolean found = false;

            while (iterator.hasNext()) {
                ScanInfo old_sInfo = iterator.next();

                if (sInfo.equals(old_sInfo)) {
                    found = true;
                    old_sInfo.addSSIDs(sInfo.getSSID());
                    break;
                }
            }

            if (!found) {
                sInfo.addSSIDs(sInfo.getSSID());
                station_count_map.put(sInfo, sInfo.getStationCount());
            }
        }

        return station_count_map;
    }

    public void doScan(View view) throws IllegalAccessException, ClassNotFoundException, Exception {
        int total_station = 0;
        String ssid_filter = null;
        List<ScanInfo> displayList = new ArrayList<>();
        List<ScanResult> apList = scanNeighbourAP();

        if (apList == null) {
            /* no scan data or wifi not enabled */
            showToast("No AP found");
            clearCountAndList();
            return;
        }

        HashMap<ScanInfo, Integer> station_count_map = parseScanResults(apList);
        List<ScanInfo> listScanInfo = new ArrayList<ScanInfo>(station_count_map.keySet());

        /* get text input: to filter SSID */
        EditText et = (EditText)findViewById(R.id.ssid_filter);
        ssid_filter = et.getText().toString();
        Log.i(LOGNAME, "ssidfilter => " + ssid_filter);

        /* sort scaninfo */
        Collections.sort(listScanInfo);

        /* iterate through scaninfo */
        Iterator<ScanInfo> iterator = listScanInfo.iterator();
        while (iterator.hasNext()) {
            ScanInfo sInfo = iterator.next();
            boolean foundSSID = false;

            Iterator<String> iterString = sInfo.getSSIDs().iterator();
            while (iterString.hasNext()) {
                String str = iterString.next();
                if (str.contains(ssid_filter)) {
                    foundSSID = true;
                    break;
                }
            }

            if (ssid_filter != null && ssid_filter != "" && !foundSSID) {
                continue;
            }

            total_station += sInfo.getStationCount();
            displayList.add(sInfo);
        }

        showToast("Total SSIDs: " + apList.size() +
                "\nAPs with count: " + listScanInfo.size() +
                "\nDisplayed APs: " + displayList.size());

        /* update station count */
        TextView tv = (TextView) findViewById(R.id.station_count);
        tv.setText(String.valueOf(total_station) + " ");

        ListView lv = (ListView) findViewById(R.id.scan_listview);
        ScanInfoAdapter adapter = new ScanInfoAdapter(this, R.layout.scanlist_item, displayList);
        lv.setAdapter(adapter);
    }
}
