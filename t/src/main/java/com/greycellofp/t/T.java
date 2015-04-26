package com.greycellofp.t;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

import com.greycellofp.t.core.Constants;
import com.greycellofp.t.utils.BitHack;
import com.greycellofp.t.utils.Calibrate;
import com.greycellofp.t.utils.ToneGenerator;
import com.greycellofp.t.utils.fft.FFT;
import static java.lang.Math.signum;

/**
 * Created by pawan.kumar1 on 25/04/15.
 */
public class T {
    private static final String TAG = T.class.getSimpleName();
    
    private AudioRecord audioRecorder;
    private ToneGenerator toneGenerator;
    private Handler handler;
    private FFT fft;
    private Calibrate calibrate;
    private GestureListener gestureListener;
    private RawBWListener rawBWListener;
    
    private short[] buffer;
    private float[] fftRealArray;

    private float[] oldFreq;
    private int bufferSize = 2048;

    public static double maxVolRatio = Constants.PEAK_VOL_RATIO;

    private float frequency;
    private int freqIndex;

    private boolean continueReading;

    private int previousDirection = 0;
    private int directionChanges;
    private int cyclesToRefresh;
    private int cyclesLeftToRead = -1;

    public T() {
        handler = new Handler();
        bufferSize = AudioRecord.getMinBufferSize(Constants.SAMPLE_RATE, 
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        buffer = new short[bufferSize];

        calibrate = new Calibrate();
        toneGenerator = ToneGenerator.construct()
                .withFrequencyOfTone(Constants.PRELIM_FREQ)
                .withDuration(5)
                .withSampleRate(Constants.SAMPLE_RATE)
                .build();
        audioRecorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, Constants.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    }

    public boolean start(){
        toneGenerator.play();
        try {
            audioRecorder.startRecording();
            continueReading = true;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    readAndFFT();
                    int minInd = fft.freqToIndex(Constants.FREQ_MIN);
                    int maxInd = fft.freqToIndex(Constants.FREQ_MAX);

                    int primaryInd = freqIndex;
                    for (int i = minInd; i <= maxInd; ++i) {
                        if (fft.getBand(i) > fft.getBand(primaryInd)) {
                            primaryInd = i;
                        }
                    }
                    T.this.frequency = fft.indexToFreq(primaryInd);
                    T.this.freqIndex = fft.freqToIndex(fft.indexToFreq(primaryInd));
                    Log.d(TAG, "Primary index:" + fft.indexToFreq(primaryInd));
                    
                    readMic();
                }
            }, 1000);

            int bufferReadResult = audioRecorder.read(buffer, 0, bufferSize);
            bufferReadResult = BitHack.npot(bufferReadResult);

            fftRealArray = new float[BitHack.npot(bufferReadResult)];
            fft = new FFT(BitHack.npot(bufferReadResult), Constants.SAMPLE_RATE);
        } catch (Exception e) {
            Log.e(TAG, "", e);
            toneGenerator.pause();
            return false;
        }
        return true;
    }
    
    public boolean pause(){
        try {
            audioRecorder.stop();
            toneGenerator.pause();
            continueReading = false;
            return true;
        } catch (Exception e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public GestureListener getGestureListener() {
        return gestureListener;
    }

    public void setGestureListener(GestureListener gestureListener) {
        this.gestureListener = gestureListener;
    }

    public RawBWListener getRawBWListener() {
        return rawBWListener;
    }

    public void setRawBWListener(RawBWListener rawBWListener) {
        this.rawBWListener = rawBWListener;
    }

    private void readAndFFT() {
        if (fft.specSize() != 0 && oldFreq == null) {
            oldFreq = new float[fft.specSize()];
        }
        for (int i = 0; i < fft.specSize(); ++i) {
            oldFreq[i] = fft.getBand(i);
        }

        int bufferReadResult = audioRecorder.read(buffer, 0, bufferSize);

        for (int i = 0; i < bufferReadResult; i++) {
            fftRealArray[i] = (float) buffer[i] / Short.MAX_VALUE; //32768.0
        }

        //apply windowing
        for (int i = 0; i < bufferReadResult/2; ++i) {
            float winval = (float)(0.5+0.5*Math.cos(Math.PI*(float)i/(float)(bufferReadResult/2)));
            if (i > bufferReadResult/2)  winval = 0;
            fftRealArray[bufferReadResult/2 + i] *= winval;
            fftRealArray[bufferReadResult/2 - i] *= winval;
        }

        fft.forward(fftRealArray);

        //apply smoothing
        for (int i = 0; i < fft.specSize(); ++i) {
            float smoothedOutMag = Constants.SMOOTHING_TIME_CONSTANT * fft.getBand(i) +
                    (1 - Constants.SMOOTHING_TIME_CONSTANT) * oldFreq[i];
            fft.setBand(i, smoothedOutMag);
        }
    }

    private void readMic() {
        int[] bandwidths = getBandwidth();
        int leftBandwidth = bandwidths[Constants.LEFT_BW];
        int rightBandwidth = bandwidths[Constants.RIGHT_BW];

        Log.d(TAG, "left-bw:" + leftBandwidth + " right-bw:" + rightBandwidth);
        
        if(rawBWListener != null){
            rawBWListener.onBandWidth(leftBandwidth, rightBandwidth);
        }
        
        if(gestureListener != null){
            applyGesture(leftBandwidth, rightBandwidth);
        }
        calibrate.calibrate(maxVolRatio, leftBandwidth, rightBandwidth);
        if (continueReading) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    readMic();
                }
            });
        }
    }

    private int[] getBandwidth() {
        readAndFFT();

        int primaryTone = freqIndex;

        double normalizedVolume = 0;

        double primaryVolume = fft.getBand(primaryTone);

        int leftBandwidth = 0;

        do {
            leftBandwidth++;
            double volume = fft.getBand(primaryTone - leftBandwidth);
            normalizedVolume = volume/primaryVolume;
        } while (normalizedVolume > maxVolRatio && leftBandwidth < Constants.RELEVANT_FREQ_WINDOW);


        int secondScanFlag = 0;
        int secondaryLeftBandwidth = leftBandwidth;

        //second scan
        do {
            secondaryLeftBandwidth++;
            double volume = fft.getBand(primaryTone - secondaryLeftBandwidth);
            normalizedVolume = volume/primaryVolume;

            if (normalizedVolume > Constants.SECOND_PEAK_VOL_RATIO) {
                secondScanFlag = 1;
            }

            if (secondScanFlag == 1 && normalizedVolume < maxVolRatio ) {
                break;
            }
        } while (secondaryLeftBandwidth < Constants.RELEVANT_FREQ_WINDOW);

        if (secondScanFlag == 1) {
            leftBandwidth = secondaryLeftBandwidth;
        }

        int rightBandwidth = 0;

        do {
            rightBandwidth++;
            double volume = fft.getBand(primaryTone + rightBandwidth);
            normalizedVolume = volume/primaryVolume;
        } while (normalizedVolume > maxVolRatio && rightBandwidth < Constants.RELEVANT_FREQ_WINDOW);

        secondScanFlag = 0;
        int secondaryRightBandwidth = 0;
        do {
            secondaryRightBandwidth++;
            double volume = fft.getBand(primaryTone + secondaryRightBandwidth);
            normalizedVolume = volume/primaryVolume;

            if (normalizedVolume > Constants.SECOND_PEAK_VOL_RATIO) {
                secondScanFlag = 1;
            }

            if (secondScanFlag == 1 && normalizedVolume < maxVolRatio) {
                break;
            }
        } while (secondaryRightBandwidth < Constants.RELEVANT_FREQ_WINDOW);

        if (secondScanFlag == 1) {
            rightBandwidth = secondaryRightBandwidth;
        }

        return new int[]{leftBandwidth, rightBandwidth};
    }
    
    public void applyGesture(int leftBandwidth, int rightBandwidth){
        //early escape if need to refresh
        if (cyclesToRefresh > 0) {
            cyclesToRefresh--;
            return;
        }

        int cyclesToRead = 5;
        if (leftBandwidth > 4 || rightBandwidth > 4) {
            Log.d("GESTURE CALLBACK", "Start of if statement");
            //implement gesture logic
            int difference = leftBandwidth - rightBandwidth;
            int direction = (int) signum(difference);

            //Log.d("GESTURE CALLBACK", "DIRECTION IS " + direction);
            if (direction == 1) {
                Log.d("DIRECTION", "POS");
            } else if (direction == -1) {
                Log.d("Direction", "NEG");
            } else {
                Log.d("DIrection", "none");
            }

            if (direction != 0 && direction != previousDirection) {
                //scan a 4 frame window to wait for taps or double taps
                Log.d("GESTURE CALLBACK", "previous direction is diff than current");
                cyclesLeftToRead = cyclesToRead;
                Log.d("GESTURE CALLBACK", "setting prev direction");
                previousDirection = direction;
                directionChanges++;
            }
        }

        cyclesLeftToRead--;

        if (cyclesLeftToRead == 0) {
            Log.d("GESTURE CALLBACK", "No more cycles to read. finding appropriate listener");
            if (directionChanges == 1) {
                if (previousDirection == -1) {
                    gestureListener.onPositive(leftBandwidth - rightBandwidth);
                    Log.d("GESTURE CALLBACK", "positive:" + (leftBandwidth - rightBandwidth));
                } else {
                    gestureListener.onNegative(leftBandwidth - rightBandwidth);
                    Log.d("GESTURE CALLBACK", "negative:" + (leftBandwidth - rightBandwidth));
                }
            } else if (directionChanges == 2) {
                gestureListener.onTap();
            } else {
                gestureListener.onDoubleTap();
            }
            previousDirection = 0;
            directionChanges = 0;
            cyclesToRefresh = cyclesToRead;
        } else {
            gestureListener.onNothing();
        }
    }
    
    public static interface RawBWListener{
        public void onBandWidth(int leftBandwidth, int rightBandwidth);
    }
    
    public static interface GestureListener{
        public void onPositive(int diff);
        public void onNegative(int diff);
        public void onTap();
        public void onDoubleTap();
        public void onNothing();
    }
}
