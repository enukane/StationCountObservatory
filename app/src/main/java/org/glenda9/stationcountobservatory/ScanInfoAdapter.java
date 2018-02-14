package org.glenda9.stationcountobservatory;

import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import java.util.List;

public class ScanInfoAdapter extends ArrayAdapter<ScanInfo>{
    private List<ScanInfo> items;
    private LayoutInflater inflater;
    private int resource;

    public ScanInfoAdapter(Context context, int resource, List<ScanInfo> items) {
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

        ScanInfo sInfo = items.get(position);

        /* fill device name */
        tv = (TextView)view.findViewById(R.id.per_device_name);
        tv.setText(sInfo.getDeviceName());

        tv = (TextView)view.findViewById(R.id.per_device_count);
        tv.setText(String.valueOf(sInfo.getStationCount()));

        tv = (TextView)view.findViewById(R.id.per_device_bssid);
        tv.setText(sInfo.getBSSID());

        tv = (TextView)view.findViewById(R.id.per_device_ssid);
        tv.setText(sInfo.getPrettySSIDs());

        tv = (TextView)view.findViewById(R.id.per_device_freq);
        tv.setText(String.valueOf(sInfo.getFreq()));

        return view;
    }
}
