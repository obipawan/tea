package com.greycellofp.tdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.greycellofp.t.T;

/**
 * Created by pawan.kumar1 on 25/04/15.
 */
public class PlaceholderFragment extends Fragment {
    private static final String TAG = PlaceholderFragment.class.getSimpleName();
    
    private ListView listView;
    T t;
    public PlaceholderFragment() {
        t = new T();
        t.setGestureListener(new T.GestureListener() {
            @Override
            public void onPositive(int i) {
                if(i < -1){
                    Log.d(TAG, "negative:" + i);
                    listView.smoothScrollByOffset(i);
                }
            }

            @Override
            public void onNegative(int i) {
                if(i > 1){
                    Log.d(TAG, "negative:" + i);
                    listView.smoothScrollByOffset(i);
                }
            }

            @Override
            public void onTap() {
//                Log.d(TAG, "onTap");
            }

            @Override
            public void onDoubleTap() {
//                Log.d(TAG, "onDoubleTap:");
            }

            @Override
            public void onNothing() {
                Log.d(TAG, "onNothing:");
            }
        });
//            t.setRawBWListener(new T.RawBWListener() {
//                @Override
//                public void onBandWidth(int leftBandwidth, int rightBandwidth) {
//                    int threshold = 4;
//                    int scale    = 10;
//                    int baseSize = 100;
//                    if (leftBandwidth > threshold || rightBandwidth > threshold) {
//                        int diff = leftBandwidth - rightBandwidth;
//                        int dimension = baseSize + scale * diff;
//                        String diffStr = "";
//                        for(int i=0; i<dimension; i++) {
//                            diffStr +="@";
//                        }
//                        Log.d("dimension" + dimension + diffStr, );
//                    }
//                    Log.d(TAG, "left bandwidth:" + leftBandwidth + " right bandwidth:" + rightBandwidth);
//                }
//            });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        listView = (ListView) rootView.findViewById(R.id.list_view);
        listView.setAdapter(new Adapter(getActivity()));
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        t.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        t.pause();
    }
}
