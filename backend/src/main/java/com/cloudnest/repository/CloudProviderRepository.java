package com.cloudnest.repository;

import com.cloudnest.entity.CloudProvider;
import com.cloudnest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CloudProviderRepository extends JpaRepository<CloudProvider, Long> {
    List<CloudProvider> findByUser(User user);
    Optional<CloudProvider> findByIdAndUser(Long id, User user);
}
