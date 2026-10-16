package com.yourid.api.analytics.repository;

import com.yourid.api.analytics.entity.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {
    List<AnalyticsEvent> findByStoreIdAndCreatedAtAfter(UUID storeId, Instant since);
}
