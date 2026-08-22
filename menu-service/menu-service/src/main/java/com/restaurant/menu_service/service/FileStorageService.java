package com.restaurant.menu_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadPath;

    public FileStorageService(
            @Value("${app.file.upload-dir}") String uploadDir
    ) {

        this.uploadPath =
                Paths.get(uploadDir)
                        .toAbsolutePath()
                        .normalize();

        try {

            Files.createDirectories(uploadPath);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not create upload directory",
                    e
            );
        }
    }


    public String storeFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new RuntimeException(
                    "Please select an image"
            );
        }


        String contentType =
                file.getContentType();


        if (contentType == null ||
                !isAllowedImageType(contentType)) {

            throw new RuntimeException(
                    "Only JPG, JPEG, PNG and WEBP images are allowed"
            );
        }


        String originalFilename =
                StringUtils.cleanPath(
                        file.getOriginalFilename()
                );


        String extension =
                getExtension(originalFilename);


        String fileName =
                UUID.randomUUID()
                        + extension;


        try {

            Path targetLocation =
                    uploadPath.resolve(fileName)
                            .normalize();


            if (!targetLocation.getParent()
                    .equals(uploadPath)) {

                throw new RuntimeException(
                        "Invalid file path"
                );
            }


            Files.copy(
                    file.getInputStream(),
                    targetLocation,
                    StandardCopyOption.REPLACE_EXISTING
            );


            return fileName;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not store file",
                    e
            );
        }
    }


    private boolean isAllowedImageType(
            String contentType
    ) {

        return contentType.equals("image/jpeg")
                || contentType.equals("image/png")
                || contentType.equals("image/webp");
    }


    private String getExtension(
            String filename
    ) {

        int index =
                filename.lastIndexOf('.');


        if (index == -1) {

            throw new RuntimeException(
                    "Image file must have an extension"
            );
        }


        return filename.substring(index)
                .toLowerCase();
    }


    public void deleteFile(String fileName) {

        if (fileName == null ||
                fileName.isBlank()) {

            return;
        }


        try {

            Path filePath =
                    uploadPath.resolve(fileName)
                            .normalize();


            if (filePath.getParent()
                    .equals(uploadPath)) {

                Files.deleteIfExists(filePath);
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not delete file",
                    e
            );
        }
    }
}