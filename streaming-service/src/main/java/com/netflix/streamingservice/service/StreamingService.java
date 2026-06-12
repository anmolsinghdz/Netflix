package com.netflix.streamingservice.service;

import com.netflix.streamingservice.dto.StreamingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StreamingService {

    private final S3Client s3Client;

    private final S3Presigner s3Presigner;

    private final RedisTemplate<String,String> redisTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry}")
    private long presignedUrlExpiry;  // 60 minutes

    //Redis key for caching streaming url
    private final static String STREAMING_URL_CACHE_PREFIX = "streaming:url:";

    /**
     * Get the streaming URL for the movie
     *
     * FLOW:
     * 1. Check redis cache for the existing presigned URL
     * 2. If cached - return immediately
     * 3. If not cached - generate new presigned URL from S3
     * 4. Cache the URL in Redis
     * 5. Return the existing URL
     *
     * Why presigned URl?
     * - S3 bucket is a private locker room - videos are not publicly accessible
     * - Presigned URL gives temporary access - (X minutes)
     * - Prevents unauthorized video downloads
     */


    public StreamingResponse getStreamingUrl(String movieId, String playlistKey) {
        log.info("Getting streaming URL for movie:{}",movieId);

        String cacheKey=STREAMING_URL_CACHE_PREFIX+movieId;

        //check redis cache first
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        if(cachedUrl!=null){
            return new StreamingResponse(movieId,cachedUrl,
                    "1080p, 720p, 480p, 360p", presignedUrlExpiry);
        }

        //generate presigned URL from S3
        log.info("Generating new Streaming URL for movie:{}",movieId);
        String presignedUrl=generatePresignedUrl(playlistKey);

        //cached in redis for 55 minutes
        //5 minutes less than actual expiry to avoid edge cases
        redisTemplate.opsForValue().set(cacheKey, presignedUrl,55, TimeUnit.MINUTES);

        log.info("Presigned URL generated and cached for movie:{}",movieId);
        return new StreamingResponse(movieId,presignedUrl
                ,"1080p, 720p, 480p, 360p", presignedUrlExpiry);
    }

    /**
     * This is a key method that makes everything secure
     */

    public String getSignedPlaylist(String movieId, String playlistPath) {

        //get base path for this playlist
        String basePath=playlistPath.substring(0, playlistPath.lastIndexOf("/")+1);

        //read m3u8 content from S3
        String m3u8Content=readFromS3(playlistPath);

        //rewrite each line that is a segment or playlist reference
        String signedContent=rewriteM3U8SignedUrls(m3u8Content, basePath);

        return signedContent;
    }

    /**
     * Read file content from S3
     * @param s3Key
     * @return
     */

    private String readFromS3(String s3Key) {
        GetObjectRequest request=GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        ResponseInputStream<GetObjectResponse> response=s3Client.getObject(request);

        return new BufferedReader(new InputStreamReader(response))
                .lines()
                .collect(Collectors.joining("\n"));

    }

    private String rewriteM3U8SignedUrls(String m3u8Content, String basePath) {
        StringBuilder rewritten=new StringBuilder();

        for(String line:m3u8Content.split("\n")){
            String trimmed = line.trim();

            //skip empty lines and comments

            if(trimmed.isEmpty() || trimmed.startsWith("#")){
                rewritten.append(trimmed).append("\n");
                continue;
            }

            //This is a segment or playlist reference
            //Build full S3 key and sign it

            String fullKey=basePath+trimmed;
            String signedUrl = generatePresignedUrl(fullKey);
            rewritten.append(signedUrl).append("\n");
        }
        return rewritten.toString();
    }

    /**
     * Generate a presigned URL for S3 object
     * URL expired after configured time
     */

    private String generatePresignedUrl(String key) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest=GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiry))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    /**
     * Invalidate cache streaming URL
     * Called when video is re-encoded or updated
     */

    private void invalidateCache(String movieId) {
        String cacheKey=STREAMING_URL_CACHE_PREFIX+movieId;
        redisTemplate.delete(cacheKey);
        log.info("Streaming URL cache invalidated for movie:{}",movieId);
    }

}
