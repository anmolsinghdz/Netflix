package com.netflix.videoservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published to Kafka when a video is uploaded to S3
 * Encoding Service consumes this to start Ffmpeg processing.
 *
 * TOPIC: video.uploaded
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadedEvent {
    private String movieId;
    private String videoKey;
    private String bucketName;
    private String originalFileName;
    private long fileSizeBytes;
}
