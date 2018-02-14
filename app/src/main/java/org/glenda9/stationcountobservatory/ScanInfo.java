package org.glenda9.stationcountobservatory;

import java.util.Objects;

public class ScanInfo implements Comparable<ScanInfo> {
    private int freq;
    private String ssid;
    private String bssid;
    private String device_name;
    private int station_count;

    public ScanInfo(int freq, String ssid, String bssid, String device_name, int station_count) {
        this.freq = freq;
        this.ssid = ssid;
        this.bssid = bssid;
        this.device_name = device_name;
        this.station_count = station_count;
    }

    public String toPrettyString() {
        return "\"" + this.device_name + "\"\n" +
                "Count: " + this.station_count + "\n" +
                "BSSID=" + this.bssid + ", SSID=" + this.ssid + ", FREQ=" + this.freq;
    }

    public int getFreq() {
        return this.freq;
    }

    public String getSSID() {
        return this.ssid;
    }

    public String getBSSID() {
        return this.bssid;
    }

    public String getDeviceName() {
        return this.device_name;
    }

    public int getStationCount() {
        return this.station_count;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScanInfo) {
            ScanInfo sinfo = (ScanInfo) obj;
            return this.freq == sinfo.freq && this.device_name.equals(sinfo.device_name);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(freq, device_name);
    }

    @Override
    public int compareTo(ScanInfo otherScanInfo) {
        return this.bssid.compareTo(otherScanInfo.getBSSID());
    }
}

