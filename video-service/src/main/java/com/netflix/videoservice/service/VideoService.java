package com.netflix.videoservice.service;

import com.netflix.videoservice.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoService {

    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private static final String VIDEO_UPLOADED_TOPIC="video-uploaded";

    /**
     * FLOW:
     * 1. Receive multipart video file
     * 2. Generate unique s3 key
     * 3. Upload to s3
     * 4. Publish VideoUploadedEvent to kafka
     * 5. Encoding service picks up and starts Ffmpeg
     */

    public String uploadVideo(String movieId, MultipartFile file) throws IOException {
        log.info("Starting video upload for movie: {} file: {}", movieId, file.getOriginalFilename());

        //Generate unique S3 key for raw video
        //Format: raw/movieId/uuid_file_name

        String videoKey="raw/"+movieId+"/"+ UUID.randomUUID()+"_"+file.getOriginalFilename();

        PutObjectRequest putObjectRequest=PutObjectRequest.builder()
                .bucket(bucketName)
                .key(videoKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        log.info("Video uploaded to S3 successfully. Key: {}", videoKey);

        //publish the event to Kafka
        //Encoding Service will consume this event and start Ffmpeg processing

        VideoUploadedEvent event=new VideoUploadedEvent(
                movieId,
                videoKey,
                bucketName,
                file.getOriginalFilename(),
                file.getSize()
        );

        kafkaTemplate.send(VIDEO_UPLOADED_TOPIC, movieId, event);

        log.info("VideoUploaded event published for movie: {}", movieId);
        return videoKey;
    }
}
