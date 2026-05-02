package com.mockly.core.service;

import com.mockly.core.dto.ml.MLProcessRequest;
import com.mockly.core.dto.ml.MLProcessResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;





@Service
@Slf4j
public class MLServiceClient {

    private final WebClient mlServiceWebClient;

    public MLServiceClient(@Qualifier("mlServiceWebClient") WebClient mlServiceWebClient) {
        this.mlServiceWebClient = mlServiceWebClient;
    }

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(30); 

    





    public MLProcessResponse processArtifact(MLProcessRequest request) {
        log.info("Sending artifact to ML service for processing: sessionId={}, artifactId={}", 
                request.sessionId(), request.artifactId());

        try {
            MLProcessResponse response = processViaEvaluateEndpoint(request);
            log.info("ML service processing completed for session: {}", request.sessionId());
            return response;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                
                log.warn("ML endpoint /api/evaluate not found, falling back to legacy /api/process");
                return processViaLegacyEndpoint(request);
            }
            log.error("ML service error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("ML service processing failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling ML service", e);
            throw new RuntimeException("Failed to process artifact with ML service", e);
        }
    }

    




    public boolean isAvailable() {
        return checkHealthEndpoint("/ping") || checkHealthEndpoint("/health");
    }

    private MLProcessResponse processViaEvaluateEndpoint(MLProcessRequest request) {
        EvaluateRequest evaluateRequest = new EvaluateRequest(
                request.sessionId().toString(),
                request.artifactUrl(),
                "en",
                null
        );

        EvaluateResponse response = mlServiceWebClient.post()
                .uri("/api/evaluate")
                .bodyValue(evaluateRequest)
                .retrieve()
                .bodyToMono(EvaluateResponse.class)
                .timeout(REQUEST_TIMEOUT)
                .block();

        if (response == null) {
            throw new RuntimeException("ML service returned empty response");
        }

        Map<String, Object> transcript = null;
        if (response.transcript() != null && !response.transcript().isBlank()) {
            transcript = Map.of("text", response.transcript());
        }

        return new MLProcessResponse(
                toMetrics(response),
                response.summary(),
                response.recommendations(),
                transcript
        );
    }

    private MLProcessResponse processViaLegacyEndpoint(MLProcessRequest request) {
        try {
            MLProcessResponse response = mlServiceWebClient.post()
                    .uri("/api/process")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MLProcessResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null) {
                throw new RuntimeException("Legacy ML endpoint returned empty response");
            }
            return response;
        } catch (WebClientResponseException e) {
            log.error("Legacy ML service error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("ML service processing failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> toMetrics(EvaluateResponse response) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("overallScore", response.overallScore());
        metrics.put("overallLabel", response.overallLabel());
        metrics.put("overallMessage", response.overallMessage());
        metrics.put("strengths", response.strengths() != null ? response.strengths() : List.of());
        metrics.put("areasToImprove", response.areasToImprove() != null ? response.areasToImprove() : List.of());

        if (response.speechAnalysis() != null) {
            Map<String, Object> speechAnalysis = new LinkedHashMap<>();
            speechAnalysis.put("paceLabel", response.speechAnalysis().paceLabel());
            speechAnalysis.put("paceScore", response.speechAnalysis().paceScore());
            speechAnalysis.put("fillerWordsCount", response.speechAnalysis().fillerWordsCount());
            speechAnalysis.put("fillerWordRate", response.speechAnalysis().fillerWordRate());
            metrics.put("speechAnalysis", speechAnalysis);
        }

        if (response.scores() != null) {
            Map<String, Object> scores = new LinkedHashMap<>();
            scores.put("communication", response.scores().communication());
            scores.put("technical", response.scores().technical());
            scores.put("confidence", response.scores().confidence());
            metrics.put("scores", scores);
        }

        return metrics;
    }

    private boolean checkHealthEndpoint(String path) {
        try {
            mlServiceWebClient.get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            log.debug("ML service health check failed for {}", path, e);
            return false;
        }
    }

    private record EvaluateRequest(
            String sessionId,
            String artifactUrl,
            String language,
            String transcript
    ) {}

    private record EvaluateResponse(
            Double overallScore,
            String overallLabel,
            String overallMessage,
            List<String> strengths,
            List<String> areasToImprove,
            SpeechAnalysis speechAnalysis,
            String summary,
            String recommendations,
            Scores scores,
            String transcript
    ) {}

    private record SpeechAnalysis(
            String paceLabel,
            Double paceScore,
            Integer fillerWordsCount,
            Double fillerWordRate
    ) {}

    private record Scores(
            Double communication,
            Double technical,
            Double confidence
    ) {}
}

