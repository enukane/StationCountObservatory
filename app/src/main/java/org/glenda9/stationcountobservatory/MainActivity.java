package org.glenda9.stationcountobservatory;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcel;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.EditText;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    public static final String LOGNAME="scobservatory";
    public static final int IE_ID_CC1X=133;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(LOGNAME, "starting");
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
            return null;
        }

        manager.startScan();
        return manager.getScanResults();
    }

    public ScanInfo parseScanResultToScanInfo(ScanResult sr) {
        ScanInfo sInfo = null;

        /*
        Accessing Information Elemenents through parcel.
        We expected ScanResult.bytes to be valid and accessible through reflection,
        but actually it's empty. Only valid Information Element is in its private class
        InformationElement (through informationElements member), but this is rather hard.
        So here, we use Parce to parse IE and find CC1X + DeviceName.
         */
        Parcel out = Parcel.obtain();
        sr.writeToParcel(out, 0);
        out.setDataPosition(0);

        int ssid_parcel = out.readInt();
        if (ssid_parcel == 1) {
            int length = out.readInt();
            byte b[] = new byte[length];
            out.readByteArray(b);
        }

        String ssid = out.readString(); /** use later */
        Log.i(LOGNAME, "ssid => " + ssid);
        String bssid = out.readString(); /** use later */
        Long hessid = out.readLong();
        int anqpdomain =  out.readInt();
        String capability = out.readString();
        int level =  out.readInt();
        int freq = out.readInt(); /** use later */
        Long timestamp = out.readLong();
        int distanceCm = out.readInt();
        int distanceSdCm = out.readInt();
        int channelWidth = out.readInt();
        int freq0 = out.readInt();
        int freq1 = out.readInt();
        Long seen = out.readLong();
        int untrusted = out.readInt();
        int numconnections = out.readInt();
        int numusage = out.readInt();
        int numipconffail = out.readInt();
        int isautojoincandidate = out.readInt();
        String venuename = out.readString();
        String opfriendlyname = out.readString();
        Long flags = out.readLong();

        int ienum = out.readInt();
        for (int j = 0; j < ienum; j++) {
            int ie_id = out.readInt();
            int ie_len = out.readInt();
            byte[] ie_buf = new byte[ie_len];
            out.readByteArray(ie_buf);

            if (ie_id != IE_ID_CC1X) {
                continue;
            }

            int unknown1_idx = 0;
            int device_name_idx = unknown1_idx + 10;
            int station_count_idx = device_name_idx + 16;
            int device_name_end_idx = station_count_idx;


            for (int idx = device_name_idx; idx < station_count_idx; idx ++) {
                if (ie_buf[idx] != 0) {
                    continue;
                }
                device_name_end_idx = idx;
                break;
            }

            byte[] device_name_bytes = Arrays.copyOfRange(ie_buf, device_name_idx, device_name_end_idx);
            int station_count = (int)ie_buf[station_count_idx];
            String device_name = new String(device_name_bytes);

            sInfo = new ScanInfo(freq, ssid, bssid, device_name, station_count);

            Log.i(LOGNAME, sInfo.toPrettyString());
        }

        return sInfo;
    }

    public HashMap<ScanInfo, Integer> parseScanResults(List<ScanResult> apList) {
        HashMap<ScanInfo, Integer> station_count_map = new HashMap<ScanInfo, Integer>();

        if (apList == null) {
            return null;
        }

        for (int i = 0; i < apList.size(); i++) {
            ScanResult sr = apList.get(i);
            ScanInfo sInfo = parseScanResultToScanInfo(sr);
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
                station_count_map.put(sInfo, sInfo.getStationCount());
            }
        }

        return station_count_map;
    }

    public void doScan(View view) throws IllegalAccessException, ClassNotFoundException {
        int total_station = 0;
        String ssid_filter = null;

        List<ScanInfo> displayList = new ArrayList<>();
        List<ScanResult> apList = scanNeighbourAP();
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

            if (ssid_filter != null && ssid_filter != "" && !sInfo.getSSID().contains(ssid_filter)) {
                continue;
            }

            total_station += sInfo.getStationCount();
            displayList.add(sInfo);
        }

        /* update station count */
        TextView tv = (TextView) findViewById(R.id.station_count);
        tv.setText(String.valueOf(total_station) + " ");

        ListView lv = (ListView) findViewById(R.id.scan_listview);
        ScanInfoAdapter adapter = new ScanInfoAdapter(this, R.layout.scanlist_item, displayList);
        lv.setAdapter(adapter);
    }
}
