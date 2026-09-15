package com.ebike.marketplaceModule.entity;

import com.ebike.authModule.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_listings", schema = "marketplace")
public class VehicleListing {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    @Column(name = "brand_id")
    private Long brandId;
    @Column(nullable = false, length = 220)
    private String title;
    @Column(nullable = false, unique = true, length = 260)
    private String slug;
    @Column(nullable = false, columnDefinition = "text")
    private String description;
    @Column(name = "listing_type", nullable = false, length = 20)
    private String listingType;
    @Column(nullable = false, length = 20)
    private String condition;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    private boolean negotiable;
    @Column(name = "exchange_allowed", nullable = false)
    private boolean exchangeAllowed;
    @Column(nullable = false, length = 120)
    private String province;
    @Column(length = 120)
    private String district;
    @Column(name = "address_text", length = 500)
    private String addressText;
    @Column(name = "view_count", nullable = false)
    private long viewCount;
    @Column(name = "favorite_count", nullable = false)
    private long favoriteCount;
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Version
    private long version;

    public VehicleListing() {}

    @PrePersist void createTimestamps() {
        OffsetDateTime now = OffsetDateTime.now();
        if (publicId == null) publicId = UUID.randomUUID();
        createdAt = now;
        updatedAt = now;
    }
    @PreUpdate void updateTimestamp() { updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getListingType() { return listingType; }
    public void setListingType(String listingType) { this.listingType = listingType; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isNegotiable() { return negotiable; }
    public void setNegotiable(boolean negotiable) { this.negotiable = negotiable; }
    public boolean isExchangeAllowed() { return exchangeAllowed; }
    public void setExchangeAllowed(boolean exchangeAllowed) { this.exchangeAllowed = exchangeAllowed; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getAddressText() { return addressText; }
    public void setAddressText(String addressText) { this.addressText = addressText; }
    public long getViewCount() { return viewCount; }
    public long getFavoriteCount() { return favoriteCount; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
