package com.firebirdberlin.tinytimetracker;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class AccessPointAdapter extends ArrayAdapter<AccessPoint> {
    private Context mContext = null;

    public AccessPointAdapter(Context context, int viewid, List<AccessPoint> values) {
        super(context, viewid, R.id.text1, values);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent ) {
        super.getView(position, convertView, parent);
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View v = inflater.inflate(R.layout.list_2_lines, parent, false);
        TextView text1 = ( TextView ) v.findViewById ( R.id.text1 );
        TextView text2 = ( TextView ) v.findViewById ( R.id.text2 );
        if (position < getCount()){
            AccessPoint accessPoint = (AccessPoint) getItem(position);
            text1.setText(accessPoint.ssid);
            text2.setText(accessPoint.bssid);
        }

        return v;
    }

    public boolean addUnique(AccessPoint accessPoint) {
        int index = indexOfBSSID(accessPoint.bssid);

        if (index == -1) {
            add(accessPoint);
        }
        return (index == -1);
    }

    public int indexOfBSSID(String bssid) {
        for (int i = 0; i < getCount() ; i++ ) {
            AccessPoint ap = getItem(i);
            if (ap.bssid.equals(bssid) ) {
                return i;
            }
        }
        return -1;
    }
}