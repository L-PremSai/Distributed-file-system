package com.medicalstorage.util;

import com.medicalstorage.config.SeaweedFSProperties;
import com.medicalstorage.exception.SeaweedFSException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Low-level HTTP client that talks directly to SeaweedFS.
 *
 * SeaweedFS write flow:
 *   1.  GET  master:9333/dir/assign          → returns { fid, url, publicUrl, ... }
 *   2.  POST volume-url/{fid}  (multipart)   → stores the file; returns { size, eTag }
 *
 * SeaweedFS read flow:
 *   GET  volume-url/{fid}                    → binary file bytes
 *
 * SeaweedFS delete flow:
 *   DELETE volume-url/{fid}
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeaweedFSClient {

    private final RestTemplate restTemplate;
    private final SeaweedFSProperties props;

    // ─────────────────────────────────────────────────────────
    // ASSIGN – obtain a new File ID from the master
    // ─────────────────────────────────────────────────────────

    /**
     * Calls /dir/assign on the SeaweedFS master to obtain a new (fid, url) pair.
     *
     * @return AssignResult containing fid and the volume-node URL to upload to
     */
    public AssignResult assign() {
        String assignUrl = props.getMasterUrl() + props.getAssignEndpoint();

        // Optionally request a specific replication factor
        if (props.getReplication() != null && !props.getReplication().isBlank()) {
            assignUrl += "?replication=" + props.getReplication();
        }

        log.debug("Assigning new FID from: {}", assignUrl);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(assignUrl, Map.class);

            if (response == null || !response.containsKey("fid")) {
                throw new SeaweedFSException("SeaweedFS assign returned null or missing 'fid'");
            }

            String fid = (String) response.get("fid");
            // SeaweedFS returns the volume-node URL in the "url" field (host:port, no scheme)
            String volumeUrl = "http://" + response.get("url");

            log.debug("Assigned FID={} on volume={}", fid, volumeUrl);
            return new AssignResult(fid, volumeUrl);

        } catch (HttpClientErrorException ex) {
            throw new SeaweedFSException(
                    "Failed to assign FID from SeaweedFS master: " + ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────────────────
    // UPLOAD – write the file to a volume node
    // ─────────────────────────────────────────────────────────

    /**
     * Uploads {@code file} to the volume node at {@code volumeUrl}/{@code fid}.
     *
     * @param volumeUrl  returned by assign()
     * @param fid        returned by assign()
     * @param file       multipart file from the HTTP request
     */
    public void upload(String volumeUrl, String fid, MultipartFile file) {
        String uploadUrl = volumeUrl + "/" + fid;
        log.debug("Uploading {} bytes to {}", file.getSize(), uploadUrl);

        try {
            // Build the multipart body SeaweedFS expects
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl, HttpMethod.POST, requestEntity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new SeaweedFSException(
                        "SeaweedFS upload failed with status: " + response.getStatusCode());
            }

            log.info("Successfully uploaded file. FID={}, size={}", fid, file.getSize());

        } catch (HttpClientErrorException ex) {
            throw new SeaweedFSException("Failed to upload file to SeaweedFS: " + ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────────────────
    // DOWNLOAD – stream the raw bytes back to the caller
    // ─────────────────────────────────────────────────────────

    /**
     * Downloads the raw bytes of a stored file.
     *
     * @param fid  the SeaweedFS file ID stored in MySQL
     * @return raw file bytes
     */
    public byte[] download(String fid) {
        String downloadUrl = buildVolumeUrl(fid);
        log.debug("Downloading from SeaweedFS: {}", downloadUrl);

        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(downloadUrl, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new SeaweedFSException("SeaweedFS returned empty response for FID: " + fid);
            }

            return response.getBody();

        } catch (HttpClientErrorException.NotFound ex) {
            throw new SeaweedFSException("File not found in SeaweedFS for FID: " + fid, ex);
        } catch (HttpClientErrorException ex) {
            throw new SeaweedFSException("Failed to download from SeaweedFS: " + ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────

    /**
     * Deletes the file from SeaweedFS volume storage.
     *
     * @param fid  the SeaweedFS file ID
     */
    public void delete(String fid) {
        String deleteUrl = buildVolumeUrl(fid);
        log.debug("Deleting from SeaweedFS: {}", deleteUrl);

        try {
            restTemplate.delete(deleteUrl);
            log.info("Deleted FID={} from SeaweedFS", fid);
        } catch (HttpClientErrorException ex) {
            // Log but don't hard-fail; the MySQL record will still be removed
            log.error("Failed to delete FID={} from SeaweedFS: {}", fid, ex.getMessage());
            throw new SeaweedFSException("Failed to delete from SeaweedFS: " + ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────────────────
    // URL helper
    // ─────────────────────────────────────────────────────────

    /**
     * Builds the public-facing URL a client can use to stream the image directly.
     */
    public String buildPublicUrl(String fid) {
        return props.getVolumeUrl() + "/" + fid;
    }

    private String buildVolumeUrl(String fid) {
        return props.getVolumeUrl() + "/" + fid;
    }

    // ─────────────────────────────────────────────────────────
    // Inner types
    // ─────────────────────────────────────────────────────────

    /**
     * Value object returned by {@link #assign()}.
     */
    public record AssignResult(String fid, String volumeUrl) {}

    /**
     * Wraps a {@link MultipartFile} as a Spring {@link ByteArrayResource}
     * so it can be included in a multipart POST body.
     */
    private static class MultipartFileResource extends ByteArrayResource {
        private final String filename;

        MultipartFileResource(MultipartFile file) throws IOException {
            super(file.getBytes());
            this.filename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "upload";
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
