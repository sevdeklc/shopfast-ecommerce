package com.shopfast.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Integer stockQuantity;
    private String category;
    private String imageUrl;
    private Boolean isActive;
    private Boolean hasActiveCampaign;
    private String campaignName;
    private BigDecimal discountPercentage;
    private LocalDateTime createdAt;
}
