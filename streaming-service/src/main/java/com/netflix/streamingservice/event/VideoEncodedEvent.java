package com.netflix.streamingservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * consumed from kafka topic: video.encoded
 * published by encoding service after Ffmpeg processing
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoEncodedEvent {
    private String movieId;
    private String hlsUrl;               // Master playlist URL for streaming
    private String masterPlaylistKey;    // S3 key master.m3u8
    private boolean success;
    private String errorMessage;
}
