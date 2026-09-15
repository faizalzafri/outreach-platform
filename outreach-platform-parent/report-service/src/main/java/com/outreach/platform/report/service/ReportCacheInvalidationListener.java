package com.outreach.platform.report.service;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for ImportJobCompleted application events and invalidates report caches.
 * This ensures analytics data stays fresh after new data is ingested.
 */
@Component
public class ReportCacheInvalidationListener {

    private static final Logger log = LoggerFactory.getLogger(ReportCacheInvalidationListener.class);

    private final ReportService reportService;

    @Inject
    public ReportCacheInvalidationListener(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Handles ImportJobCompleted events by evicting all report caches. */
    @EventListener
    public void onImportJobCompleted(ImportJobCompletedEvent event) {
        log.info("ImportJobCompleted event received (jobId={}). Evicting report caches.", event.jobId());
        reportService.evictAllCaches();
    }
}
