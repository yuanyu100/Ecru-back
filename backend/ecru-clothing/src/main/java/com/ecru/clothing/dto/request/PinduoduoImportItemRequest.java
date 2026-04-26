package com.ecru.clothing.dto.request;

import lombok.Data;

@Data
public class PinduoduoImportItemRequest {

    private String productName;
    private String productUrl;
    private String imageUrl;
    private String orderTime;
    private Double price;
    private String shopName;
    private String skuText;
    private String orderId;
    private String brand;
    private String category;
    private String material;
    private String size;
}
