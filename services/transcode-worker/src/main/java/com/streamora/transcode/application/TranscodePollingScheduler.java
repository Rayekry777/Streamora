package com.streamora.transcode.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Keeps polling transport concerns outside the worker use case. */
@Component
public class TranscodePollingScheduler {
    private final TranscodeWorkerService workerService;

    public TranscodePollingScheduler(TranscodeWorkerService workerService) {
        this.workerService = workerService;
    }

    @Scheduled(fixedDelayString = "${streamora.transcode.poll-delay-ms}")
    public void poll() {
        workerService.processOne();
    }
}
