package com.shopfast.repository;

import com.shopfast.model.entity.Campaign;
import com.shopfast.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    @Query("SELECT c FROM Campaign c WHERE c.isActive = true AND c.startDate <= :now AND c.endDate >= :now")
    List<Campaign> findActiveCampaigns(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Campaign c WHERE c.product = :product AND c.isActive = true AND c.startDate <= :now AND c.endDate >= :now")
    Optional<Campaign> findActiveCampaignByProduct(@Param("product") Product product, @Param("now") LocalDateTime now);
}
