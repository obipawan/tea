package com.greycellofp.t.utils;

/**
 * Created by pawan.kumar1 on 25/04/15.
 */
/*
source : http://graphics.stanford.edu/~seander/bithacks.html
 */
public class BitHack {
    public static int npot(int v){
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v++;
        return v;
    }
}
