package com.netflix.contentservice.model;

/**
 * tracks the video processing lifecycle
 * FLOW:
 * PENDING -> UPLOADED -> ENCODING -> ENCODED -> READY
 *                                 -> FAILED
 */

public enum VideoStatus {
    PENDING,  // movie added but not uploaded yet
    UPLOADED, // raw video uploaded to S3
    ENCODING, // Ffmpeg is encoding the video
    ENCODED,  // Encoding complete
    READY,    // HLS playlist is ready - can be streamed
    FAILED    // Encoding failed
}
