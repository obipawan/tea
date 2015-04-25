package com.greycellofp.t.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Created by pawan.kumar1 on 25/04/15.
 */
public class ToneGenerator {
    private AudioTrack audioTrack;
    private int duration;
    private int numSamples;
    private double sample[];
    private int sampleRate;
    private double freqOfTone;
    private byte generatedSound[];
    
    private ToneGenerator(){
    }
    
    public static ToneGenerator construct(){
        return new ToneGenerator();
    }
    
    public ToneGenerator withDuration(int duration){
        this.duration = duration; 
        return this;
    }
    
    public ToneGenerator withFrequencyOfTone(double freqOfTone){
        this.freqOfTone = freqOfTone;
        return this;
    }
    
    public ToneGenerator withSampleRate(int sampleRate){
        this.sampleRate = sampleRate;
        return this;
    }
    
    public ToneGenerator build(){
        numSamples = duration * sampleRate;
        sample = new double[numSamples];
        generatedSound = new byte[2 * numSamples];
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSound.length,
                AudioTrack.MODE_STREAM);
        return this;
    }

    void genTone(){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            // sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
            sample[i] = Math.sin((2 * Math.PI - .001) * i / (sampleRate/freqOfTone));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        int ramp = numSamples / 20;

        for (int i = 0; i < ramp; i++) {
            // scale to maximum amplitude
            final short val = (short) ((sample[i] * 32767) * i / ramp);
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSound[idx++] = (byte) (val & 0x00ff);
            generatedSound[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        for (int i = ramp; i < numSamples - ramp; i++) {
            // scale to maximum amplitude
            final short val = (short) ((sample[i] * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSound[idx++] = (byte) (val & 0x00ff);
            generatedSound[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        for (int i = numSamples - ramp; i < numSamples; i++) {
            // scale to maximum amplitude
            final short val = (short) ((sample[i] * 32767) * (numSamples - i) / ramp);
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSound[idx++] = (byte) (val & 0x00ff);
            generatedSound[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
    }
    
    public void play(){
        audioTrack.play();
    }
    
    public void pause(){
        audioTrack.pause();
    }
}
