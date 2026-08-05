package com.cloudnest.controller;

import com.cloudnest.dto.ScheduleRequest;
import com.cloudnest.entity.BackupSchedule;
import com.cloudnest.security.CurrentUserProvider;
import com.cloudnest.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<BackupSchedule>> list() {
        return ResponseEntity.ok(scheduleService.getUserSchedules(currentUserProvider.getCurrentUser()));
    }

    @PostMapping
    public ResponseEntity<BackupSchedule> create(@Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.createSchedule(currentUserProvider.getCurrentUser(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        scheduleService.deactivateSchedule(currentUserProvider.getCurrentUser(), id);
        return ResponseEntity.noContent().build();
    }
}
