package com.zoomstt.beta.zoombeta.utils;

import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

public class SpeechRecognizerManager {

    private final static String TAG = "SpeechRecognizerManager";
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    private Context mContext;
    protected boolean mIsListening;
    protected String language = "en-US"; //
    protected long timeout = 30000; // 20s

    private onResultsReady mListener;

    public SpeechRecognizerManager(Context context, onResultsReady listener) {
        try {
            mListener = listener;
        } catch (ClassCastException e) {
            Log.e(TAG, e.toString());
        }

        mContext = context;
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener(mContext, mListener));

        // Create new intent
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, language);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true); // For streaming result
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, timeout);

        // Start listening
        startListening();
    }

    private void startListening() {
        if (!mIsListening) {
            mIsListening = true;
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        }
    }

    public void stop() {
        if (mIsListening && mSpeechRecognizer != null) {
            mSpeechRecognizer.stopListening();
            mSpeechRecognizer.cancel();
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }

        mIsListening = false;
    }

    public void destroy() {
        mIsListening = false;
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.stopListening();
            mSpeechRecognizer.cancel();
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }

    }

    public boolean ismIsListening() {
        return mIsListening;
    }
}
