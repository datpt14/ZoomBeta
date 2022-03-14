package com.zoomstt.beta.zoombeta.audio;


import com.zoomstt.beta.zoombeta.BaseCallback;
import com.zoomstt.beta.zoombeta.BaseEvent;
import com.zoomstt.beta.zoombeta.SimpleInMeetingListener;

import us.zoom.sdk.InMeetingServiceListener;
import us.zoom.sdk.ZoomSDK;

public class MeetingAudioCallback extends BaseCallback<MeetingAudioCallback.AudioEvent> {

    public interface AudioEvent extends BaseEvent {

        void onUserAudioStatusChanged(long userId);

        void onUserAudioTypeChanged(long userId);

        void onMyAudioSourceTypeChanged(int type);
    }

    static MeetingAudioCallback instance;

    private MeetingAudioCallback() {
        if (instance != null){
            init();
        }
    }

    protected void init() {
        ZoomSDK.getInstance().getInMeetingService().addListener(audioListener);
    }

    public static MeetingAudioCallback getInstance() {
        synchronized (MeetingAudioCallback.class) {
            if (instance == null) {
                instance = new MeetingAudioCallback();
            }
        }
        return instance;
    }


    SimpleInMeetingListener audioListener = new SimpleInMeetingListener() {

        @Override
        public void onUserAudioStatusChanged(long userId, AudioStatus audioStatus) {
            for (AudioEvent event : callbacks) {
                event.onUserAudioStatusChanged(userId);
            }
        }

        @Override
        public void onUserAudioTypeChanged(long userId) {
            for (AudioEvent event : callbacks) {
                event.onUserAudioTypeChanged(userId);
            }
        }

        @Override
        public void onMyAudioSourceTypeChanged(int type) {
            for (AudioEvent event : callbacks) {
                event.onMyAudioSourceTypeChanged(type);
            }
        }
    };


}
