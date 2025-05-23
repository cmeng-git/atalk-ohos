package com.pierfrancescosoffritti.youtubeplayer.ui.utils;

public class TimeUtilities {
    /**
     * Transform the time in seconds in a string with format "M:SS".
     */
    public static String formatTime(float timeInSeconds) {
        int minutes = (int) (timeInSeconds / 60);
        int seconds = (int) (timeInSeconds % 60);
        return String.format("%d:%02d", minutes, seconds);
    }
}
