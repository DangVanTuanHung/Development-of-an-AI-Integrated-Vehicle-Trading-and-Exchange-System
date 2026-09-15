package com.ebike.marketplaceModule.repository;

import com.ebike.marketplaceModule.entity.VehicleListing;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleListingRepository extends JpaRepository<VehicleListing, Long> {
    List<VehicleListing> findByStatusOrderByPublishedAtDesc(String status);
    List<VehicleListing> findBySellerUsernameOrderByCreatedAtDesc(String username);
    Optional<VehicleListing> findByPublicId(java.util.UUID publicId);
}

