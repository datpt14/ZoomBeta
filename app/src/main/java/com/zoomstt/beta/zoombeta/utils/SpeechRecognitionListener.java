package com.zoomstt.beta.zoombeta.utils;

import android.content.Context;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class SpeechRecognitionListener implements RecognitionListener {
    private final Context mContext;
    private final onResultsReady mListener;

    public SpeechRecognitionListener(Context context, onResultsReady listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i("onBeginningOfSpeech", "Start speech");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.d("onBufferReceived", String.valueOf(buffer.length));
    }

    @Override
    public void onEndOfSpeech() {
        Log.i("onEndOfSpeech", "End speech");
    }

    @Override
    public synchronized void onError(int error) {
        Log.e("onError", String.valueOf(error));
        if (error == SpeechRecognizer.ERROR_NETWORK) {
            ArrayList<String> errorList = new ArrayList<String>(1);
            errorList.add("STOPPED LISTENING");
            if (mListener != null) {
                mListener.onResults(errorList);
                Toast.makeText(mContext, "NETWORK ERROR", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        if (partialResults != null && mListener != null) {
            ArrayList<String> texts = partialResults.getStringArrayList("android.speech.extra.UNSTABLE_TEXT");
            mListener.onStreamingResult(texts);
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
    }

    @Override
    public void onResults(Bundle results) {
        if (results != null && mListener != null) {
            ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            mListener.onResults(result);
        }
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }
}
