package com.advancedswiftly.pawfit.global.storage;

import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import com.advancedswiftly.pawfit.global.http.ExternalRestClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

public class SupabaseStorageClient implements StorageClient {

    private final PawfitProperties.Storage properties;
    private final RestClient restClient;

    public SupabaseStorageClient(PawfitProperties.Storage properties) {
        this.properties = properties;
        this.restClient = ExternalRestClientFactory.create(properties.getTimeout());
    }

    @Override
    public StoredObject upload(String path, byte[] content, String contentType) {
        ensureConfigured();
        try {
            restClient.post()
                    .uri(objectUri(path))
                    .header("apikey", properties.getServiceRoleKey())
                    .header("Authorization", "Bearer " + properties.getServiceRoleKey())
                    .header("x-upsert", "false")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
            return new StoredObject(properties.getBucket(), path, contentType, content.length);
        } catch (RestClientException exception) {
            throw new StorageException("Supabase Storage 업로드에 실패했습니다.", exception);
        }
    }

    @Override
    public URI createSignedReadUrl(String path, Duration ttl) {
        ensureConfigured();
        try {
            JsonNode response = restClient.post()
                    .uri(signUri(path))
                    .header("apikey", properties.getServiceRoleKey())
                    .header("Authorization", "Bearer " + properties.getServiceRoleKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", ttl.toSeconds()))
                    .retrieve()
                    .body(JsonNode.class);
            String signedPath = response == null ? "" : response.path("signedURL").asText("");
            if (signedPath.isBlank() && response != null) {
                signedPath = response.path("signedUrl").asText("");
            }
            if (signedPath.isBlank()) {
                throw new StorageException("Supabase Storage가 signed URL을 반환하지 않았습니다.");
            }
            if (signedPath.startsWith("https://") || signedPath.startsWith("http://")) {
                return URI.create(signedPath);
            }
            String separator = signedPath.startsWith("/") ? "" : "/";
            return URI.create(normalizedBaseUrl() + "/storage/v1" + separator + signedPath);
        } catch (RestClientException exception) {
            throw new StorageException("Supabase Storage signed URL 생성에 실패했습니다.", exception);
        }
    }

    @Override
    public void delete(String path) {
        ensureConfigured();
        try {
            restClient.delete()
                    .uri(objectUri(path))
                    .header("apikey", properties.getServiceRoleKey())
                    .header("Authorization", "Bearer " + properties.getServiceRoleKey())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new StorageException("Supabase Storage 파일 삭제에 실패했습니다.", exception);
        }
    }

    private URI objectUri(String path) {
        return URI.create(normalizedBaseUrl() + "/storage/v1/object/" + encodedPath(properties.getBucket())
                + "/" + encodedPath(path));
    }

    private URI signUri(String path) {
        return URI.create(normalizedBaseUrl() + "/storage/v1/object/sign/" + encodedPath(properties.getBucket())
                + "/" + encodedPath(path));
    }

    private String encodedPath(String path) {
        return Stream.of(path.split("/"))
                .map(segment -> UriUtils.encodePathSegment(segment, java.nio.charset.StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
    }

    private String normalizedBaseUrl() {
        return properties.getUrl().replaceAll("/+$", "");
    }

    private void ensureConfigured() {
        if (properties.getUrl().isBlank() || properties.getServiceRoleKey().isBlank()) {
            throw new StorageException("SUPABASE_URL과 SUPABASE_SERVICE_ROLE_KEY를 설정해야 합니다.");
        }
    }
}
