package com.cloudnest.repository;

import com.cloudnest.entity.CloudProvider;
import com.cloudnest.entity.QuotaAlert;
import com.cloudnest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuotaAlertRepository extends JpaRepository<QuotaAlert, Long> {

    List<QuotaAlert> findByUserOrderByTriggeredAtDesc(User user);

    List<QuotaAlert> findByUserAndAcknowledgedFalseOrderByTriggeredAtDesc(User user);

    /** The most recent unacknowledged alert for a provider (to detect state transitions). */
    Optional<QuotaAlert> findFirstByCloudProviderAndAcknowledgedFalseOrderByTriggeredAtDesc(
            CloudProvider cloudProvider);
}