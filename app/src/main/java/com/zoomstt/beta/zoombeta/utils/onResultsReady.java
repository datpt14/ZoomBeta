package com.zoomstt.beta.zoombeta.utils;

import java.util.ArrayList;

public interface onResultsReady {
    // Trả về dữ liệu khi hoàn thành nhận dạng hoặc gặp lỗi
    public void onResults(ArrayList<String> results);

    // Trả về dữ liệu mỗi khi chúng ta nói
    public void onStreamingResult(ArrayList<String> partialResults);
}
