package org.glenda9.stationcountobservatory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import android.util.Log;

import java.util.List;

/**
 * Created by enukane on 2018/02/18.
 */

public class ScanEntryQBSSAdapter extends ArrayAdapter<ScanEntry> {
    private List<ScanEntry> items;
    private LayoutInflater inflater;
    private int resource;

    public ScanEntryQBSSAdapter(Context context, int resource, List<ScanEntry> items) {
        super(context, resource, items);

        this.resource = resource;
        this.items = items;
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    };

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        TextView tv;

        if (convertView != null) {
            view = convertView;
        } else {
            view = inflater.inflate(resource, null);
        }

        ScanEntry sEntry = items.get(position);

        /* fill device name */
        tv = (TextView)view.findViewById(R.id.per_entry_bssid);
        tv.setText(sEntry.getBSSID());

        tv = (TextView)view.findViewById(R.id.per_entry_count);
        tv.setText(String.valueOf(sEntry.getStationCount()));

        tv = (TextView)view.findViewById(R.id.per_entry_ssid);
        tv.setText(sEntry.getSSID());

        tv = (TextView)view.findViewById(R.id.per_entry_freq);
        tv.setText(String.valueOf(sEntry.getFreq()));

        tv = (TextView)view.findViewById(R.id.per_entry_utilization);
        tv.setText(String.format("%1$.2f", sEntry.getUtilization()) + "%");

        tv = (TextView)view.findViewById(R.id.per_device_rssi);
        tv.setText(String.valueOf(sEntry.getRSSI()));

        return view;
    }
}
