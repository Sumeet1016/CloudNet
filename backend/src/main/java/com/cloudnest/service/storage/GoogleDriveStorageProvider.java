package com.cloudnest.service.storage;

import com.cloudnest.entity.CloudProvider;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

/**
 * Real Google Drive integration. Each CloudProvider row of type GOOGLE_DRIVE
 * stores the user's OAuth access/refresh token, obtained during the connect
 * flow in CloudProviderService. Files are uploaded into a dedicated
 * "CloudNest-Backups" folder in the user's own Drive.
 *
 * To run this for real: create OAuth 2.0 credentials in Google Cloud Console
 * (APIs & Services -> Credentials), enable the Drive API, and put the
 * client id/secret in application.yml under app.google.*
 */
@Slf4j
@Component
public class GoogleDriveStorageProvider implements CloudStorageProvider {

    @Value("${app.google.application-name}")
    private String applicationName;

    private Drive buildDriveClient(CloudProvider provider) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            com.google.api.client.auth.oauth2.Credential credential =
                    new com.google.api.client.auth.oauth2.Credential(
                            com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod())
                            .setAccessToken(provider.getAccessToken());

            return new Drive.Builder(httpTransport, GsonFactory.getDefaultInstance(), credential)
                    .setApplicationName(applicationName)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to build Google Drive client: " + e.getMessage(), e);
        }
    }

    /** Finds (or creates) the "CloudNest-Backups" folder in the user's Drive and returns its id */
    private String getOrCreateBackupFolder(Drive drive) throws IOException {
        String folderName = "CloudNest-Backups";
        FileList result = drive.files().list()
                .setQ("name='" + folderName + "' and mimeType='application/vnd.google-apps.folder' and trashed=false")
                .setSpaces("drive")
                .execute();

        List<com.google.api.services.drive.model.File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            return files.get(0).getId();
        }

        com.google.api.services.drive.model.File folderMetadata = new com.google.api.services.drive.model.File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        com.google.api.services.drive.model.File folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute();
        return folder.getId();
    }

    @Override
    public String upload(CloudProvider provider, File localFile, String targetFileName) {
        try {
            Drive drive = buildDriveClient(provider);
            String folderId = getOrCreateBackupFolder(drive);

            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(targetFileName);
            fileMetadata.setParents(Collections.singletonList(folderId));

            FileContent mediaContent = new FileContent("application/octet-stream", localFile);

            com.google.api.services.drive.model.File uploaded = drive.files()
                    .create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();

            log.info("[GoogleDrive] Uploaded file, id={}", uploaded.getId());
            // storagePath for Google Drive is the file's Drive object id
            return uploaded.getId();
        } catch (IOException e) {
            throw new RuntimeException("Google Drive upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public File download(CloudProvider provider, String storagePath) {
        try {
            Drive drive = buildDriveClient(provider);
            File tempFile = File.createTempFile("cloudnest-restore-", ".tmp");

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                drive.files().get(storagePath).executeMediaAndDownloadTo(outputStream);
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    outputStream.writeTo(fos);
                }
            }
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Google Drive download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(CloudProvider provider, String storagePath) {
        try {
            Drive drive = buildDriveClient(provider);
            drive.files().delete(storagePath).execute();
        } catch (IOException e) {
            log.warn("Failed to delete Google Drive file {}: {}", storagePath, e.getMessage());
        }
    }

    @Override
    public long getUsedStorageBytes(CloudProvider provider) {
        try {
            Drive drive = buildDriveClient(provider);
            com.google.api.services.drive.model.About about = drive.about()
                    .get()
                    .setFields("storageQuota")
                    .execute();
            return about.getStorageQuota().getUsage() != null ? about.getStorageQuota().getUsage() : 0L;
        } catch (IOException e) {
            log.warn("Could not fetch Google Drive usage: {}", e.getMessage());
            return 0L;
        }
    }
}
