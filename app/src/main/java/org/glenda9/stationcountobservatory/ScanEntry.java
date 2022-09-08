package org.glenda9.stationcountobservatory;

/**
 * Created by enukane on 2018/02/18.
 */

import java.util.ArrayList;
import java.util.Objects;

public class ScanEntry implements Comparable<ScanEntry> {
    private int freq;
    private String bssid;
    private String ssid;
    private int rssi;
    private int station_count;
    private double utilization;

    public ScanEntry(int freq, String bssid, String ssid, int rssi, int station_count, int utilization) {
        this.freq = freq;
        this.bssid = bssid;
        this.ssid = ssid;
        this.rssi = rssi;
        this.station_count = station_count;
        this.utilization = ((double)utilization) * 100.0 / 255.0;
    }

    public String toString() {
        return "Freq: " + String.valueOf(this.freq) + ", BSSID: " + this.bssid + ", SSID: " + this.ssid + ", Count: " + String.valueOf(this.station_count) + ", Util: " + String.valueOf(this.utilization);
    }

    public int getFreq() {
        return this.freq;
    }

    public String getBSSID() {
        return this.bssid;
    }

    public String getSSID() {
        return this.ssid;
    }

    public int getRSSI() { return this.rssi; }

    public int getStationCount() {
        return this.station_count;
    }

    public double getUtilization() {
        return this.utilization;
    }

    public boolean equals(Object obj) {
        if (obj instanceof ScanEntry) {
            ScanEntry scanEntry = (ScanEntry) obj;
            if (this.freq == scanEntry.freq && this.bssid == scanEntry.bssid) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(ScanEntry otherScanEntry) {
        return this.bssid.compareTo(otherScanEntry.getBSSID());
    }
}
