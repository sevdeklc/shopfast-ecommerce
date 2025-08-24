package com.shopfast.repository;

import com.shopfast.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByIsActiveTrue();

    List<Product> findByCategoryAndIsActiveTrue(String category);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0 AND p.isActive = true")
    List<Product> findInStockProducts();

    @Query("SELECT p FROM Product p WHERE p.id IN :productIds")
    List<Product> findByIdIn(@Param("productIds") List<Long> productIds);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
