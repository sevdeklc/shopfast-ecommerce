package com.shopfast.service;

import com.shopfast.model.dto.CampaignResponse;
import com.shopfast.model.entity.Campaign;
import com.shopfast.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {

    private final CampaignRepository campaignRepository;

    public List<CampaignResponse> getActiveCampaigns() {
        List<Campaign> campaigns = campaignRepository.findActiveCampaigns(LocalDateTime.now());
        return campaigns.stream()
                .filter(Campaign::isCurrentlyActive)
                .map(this::convertToResponse)
                .toList();
    }

    public CampaignResponse getCampaignById(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        return convertToResponse(campaign);
    }

    private CampaignResponse convertToResponse(Campaign campaign) {
        CampaignResponse response = new CampaignResponse();
        response.setId(campaign.getId());
        response.setName(campaign.getName());
        response.setDescription(campaign.getDescription());
        response.setProductId(campaign.getProduct().getId());
        response.setProductName(campaign.getProduct().getName());
        response.setOriginalPrice(campaign.getProduct().getPrice());
        response.setDiscountedPrice(campaign.getDiscountedPrice());
        response.setDiscountPercentage(campaign.getDiscountPercentage());
        response.setMaxQuantity(campaign.getMaxQuantity());
        response.setSoldQuantity(campaign.getSoldQuantity());
        response.setStartDate(campaign.getStartDate());
        response.setEndDate(campaign.getEndDate());
        response.setIsActive(campaign.getIsActive());

        return response;
    }
}
