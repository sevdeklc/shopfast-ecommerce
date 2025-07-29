package com.shopfast.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CampaignResponse {

    private Long id;
    private String name;
    private String description;
    private Long productId;
    private String productName;
    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;
    private BigDecimal discountPercentage;
    private Integer maxQuantity;
    private Integer soldQuantity;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
}
