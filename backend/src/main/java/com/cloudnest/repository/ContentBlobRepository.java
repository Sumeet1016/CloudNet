package com.cloudnest.repository;

import com.cloudnest.entity.CloudProvider;
import com.cloudnest.entity.ContentBlob;
import com.cloudnest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContentBlobRepository extends JpaRepository<ContentBlob, Long> {

    Optional<ContentBlob> findByUserAndCloudProviderAndChecksum(
            User user, CloudProvider cloudProvider, String checksum);

    @Query("SELECT COALESCE(SUM(b.storedSizeBytes), 0) FROM ContentBlob b WHERE b.user = :user")
    long sumStoredBytesByUser(@Param("user") User user);

    long countByUser(User user);
}