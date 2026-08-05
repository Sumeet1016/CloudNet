package com.cloudnest.service;

import com.cloudnest.entity.ActivityLog;
import com.cloudnest.entity.User;
import com.cloudnest.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public void log(User user, String action, String details) {
        log(user, action, details, ActivityLog.LogLevel.INFO);
    }

    public void log(User user, String action, String details, ActivityLog.LogLevel level) {
        ActivityLog entry = ActivityLog.builder()
                .user(user)
                .action(action)
                .details(details)
                .level(level)
                .build();
        activityLogRepository.save(entry);
    }

    public List<ActivityLog> getRecentActivity(User user) {
        return activityLogRepository.findByUserOrderByCreatedAtDesc(user);
    }
}
