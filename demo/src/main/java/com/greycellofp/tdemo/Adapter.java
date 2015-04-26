package com.greycellofp.tdemo;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Created by pawan.kumar1 on 25/04/15.
 */
public class Adapter extends BaseAdapter {
    private static final String TAG = Adapter.class.getSimpleName();

    private Context context;

    public Adapter(Context context) {
        this.context = context;
    }

    private String[] images = new String[]{
        "#1abc9c",
        "#2ecc71",
        "#3498db",
        "#9b59b6",
        "#34495e",
        "#16a085",
        "#27ae60",
        "#2980b9",
        "#f39c12",
        "#e74c3c",
        "#d35400",
        "#bdc3c7",
        "#27ae60",
        "#e74c3c"
    };
    
    @Override
    public int getCount() {
        return images.length;
    }

    @Override
    public Object getItem(int position) {
        return images[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.item, parent, false);
        }
        convertView.setBackgroundColor(Color.parseColor(getItem(position).toString()));
        return convertView;
    }
}
