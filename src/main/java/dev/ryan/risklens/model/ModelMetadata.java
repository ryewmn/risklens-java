package dev.ryan.risklens.model;

public record ModelMetadata(
        String id,
        String version,
        String artifactSha256,
        String trainedAt,
        String engine) {
}
