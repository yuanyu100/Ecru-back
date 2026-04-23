package com.ecru.clothing.dto.request;

import lombok.Data;

@Data
public class ImageUploadRequest {

    private String filename;
    private String contentType;
    private String data;
}
