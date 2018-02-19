package org.glenda9.stationcountobservatory;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class QBSSLoad extends AppCompatActivity {
    private final String LOGNAME="scobservatory";
    /* menu id */
    public static final int MENU_CC1X_DEVICENAME=10;
    public static final int MENU_QBSS_LOAD=20;

    /* Scan */
    public static final String SR_MEMBER_IES="informationElements";
    public static final String SR_IE_MEMBER_ID="id";
    public static final String SR_IE_MEMBER_BYTES="bytes";
    public static final int IE_ID_QBSSLOAD=11;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qbssload);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_CC1X_DEVICENAME, Menu.NONE, "CC1X + Device Name");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_CC1X_DEVICENAME:
                Intent intent = new Intent(QBSSLoad.this, MainActivity.class);
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
        TextView tv = (TextView) findViewById(R.id.qbss_ap_count);
        tv.setText(String.valueOf(0) + " ");

        ListView lv = (ListView) findViewById(R.id.scan_qbss_listview);
        ScanInfoAdapter adapter = new ScanInfoAdapter(this, R.layout.scanlist_qbss_item, emptyDisplayList);
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

    public ScanEntry parseScanResultToScanInfoByReflection(ScanResult sr) throws NoSuchFieldException, IllegalAccessException {
        ScanEntry scanEntry = null;
        int stationCount = 0;
        int utilizationRaw = 0;
        boolean isCountFound = false;

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
                case IE_ID_QBSSLOAD:
                    int stationCountIdx = 0;
                    int utilizationIdx = 2;
                    int capIdx = 3;

                    byte[] station_count_bytes = Arrays.copyOfRange(bytes, stationCountIdx, utilizationIdx);
                    stationCount = (int)((station_count_bytes[1] << 8) + (station_count_bytes[0]));

                    utilizationRaw = (int)bytes[utilizationIdx];

                    isCountFound = true;
                    break;
                default:
                    /* ignore */
                    break;
            }
        }

        if (!isCountFound)
            return null;

        scanEntry = new ScanEntry(sr.frequency, sr.BSSID, sr.SSID, stationCount, utilizationRaw);
        return scanEntry;
    }

    public List<ScanEntry> parseScanResults(List<ScanResult> apList) throws NoSuchFieldException, IllegalAccessException {
        List<ScanEntry> listScanEntry = new ArrayList<>();

        if (apList == null) {
            return null;
        }

        for (int i = 0; i < apList.size(); i++) {
            ScanResult scanResult = apList.get(i);
            ScanEntry scanEntry = parseScanResultToScanInfoByReflection(scanResult);
            if (scanEntry == null) {
                continue;
            }

            listScanEntry.add(scanEntry);
        }

        return listScanEntry;
    }

    public void doScan(View view) throws IllegalAccessException, ClassNotFoundException, Exception {
        String ssidFilter;

        List<ScanEntry> displayList = new ArrayList<>();
        List<ScanResult> apList = scanNeighbourAP();

        if (apList == null) {
            /* no scan data or wifi not enabled */
            showToast("No AP found");
            clearCountAndList();
            return;
        }

        List<ScanEntry> listScanEntry = parseScanResults(apList);

        /* get text input: to filter SSID */
        EditText et = (EditText)findViewById(R.id.ssid_filter);
        ssidFilter = et.getText().toString();
        Log.i(LOGNAME, "ssidfilter => " + ssidFilter);

        /* sort scaninfo */
        Collections.sort(listScanEntry);

        /* iterate through scaninfo */
        Iterator<ScanEntry> iterator = listScanEntry.iterator();
        while (iterator.hasNext()) {
            ScanEntry scanEntry = iterator.next();
            boolean foundSSID = false;

            if (scanEntry.getSSID().contains(ssidFilter)) {
                foundSSID = true;
            }

            if (ssidFilter != null && ssidFilter != "" && !foundSSID) {
                continue;
            }

            displayList.add(scanEntry);
        }

        showToast("Total SSIDs: " + apList.size() +
                "\nDisplayed APs: " + displayList.size());

        /* update AP count */
        TextView tv = (TextView) findViewById(R.id.qbss_ap_count);
        tv.setText(String.valueOf(displayList.size()) + " ");

        ListView lv = (ListView) findViewById(R.id.scan_qbss_listview);
        ScanEntryQBSSAdapter adapter = new ScanEntryQBSSAdapter(this, R.layout.scanlist_qbss_item, displayList);
        lv.setAdapter(adapter);
    }
}
