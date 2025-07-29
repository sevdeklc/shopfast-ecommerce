package com.shopfast.service;

import com.shopfast.model.dto.ProductResponse;
import com.shopfast.model.entity.Campaign;
import com.shopfast.model.entity.Product;
import com.shopfast.repository.CampaignRepository;
import com.shopfast.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    private final CampaignRepository campaignRepository;

    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findByIsActiveTrue();
        return products.stream().map(this::convertToResponse).toList();
    }

    public List<ProductResponse> getProductsByCategory(String category) {
        List<Product> products = productRepository.findByCategoryAndIsActiveTrue(category);
        return products.stream().map(this::convertToResponse).toList();
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return convertToResponse(product);
    }

    public List<ProductResponse> getInStockProducts() {
        List<Product> products = productRepository.findInStockProducts();
        return products.stream().map(this::convertToResponse).toList();
    }

    private ProductResponse convertToResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());
        response.setCategory(product.getCategory());
        response.setImageUrl(product.getImageUrl());
        response.setIsActive(product.getIsActive());
        response.setCreatedAt(product.getCreatedAt());

        // Active campaign check
        Optional<Campaign> activeCampaign = campaignRepository
                .findActiveCampaignByProduct(product, LocalDateTime.now());

        if (activeCampaign.isPresent() && activeCampaign.get().isCurrentlyActive()) {
            Campaign campaign = activeCampaign.get();
            response.setHasActiveCampaign(true);
            response.setCampaignName(campaign.getName());
            response.setDiscountPercentage(campaign.getDiscountPercentage());
            response.setDiscountedPrice(campaign.getDiscountedPrice());
        } else {
            response.setHasActiveCampaign(false);
            response.setDiscountedPrice(product.getPrice());
        }

        return response;
    }
}
