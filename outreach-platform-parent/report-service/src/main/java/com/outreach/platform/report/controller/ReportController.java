package com.outreach.platform.report.controller;

import com.outreach.platform.report.model.BeneficiaryScoreDto;
import com.outreach.platform.report.model.CityScoreDto;
import com.outreach.platform.report.model.ComparisonResultDto;
import com.outreach.platform.report.model.DashboardSummaryDto;
import com.outreach.platform.report.model.EventScoreDto;
import com.outreach.platform.report.model.HeatmapEntry;
import com.outreach.platform.report.model.KpiDto;
import com.outreach.platform.report.model.NpsResultDto;
import com.outreach.platform.report.model.ParticipationRateDto;
import com.outreach.platform.report.model.PocScoreDto;
import com.outreach.platform.report.model.ReportQueryParams;
import com.outreach.platform.report.model.ScoreCountDto;
import com.outreach.platform.report.model.SentimentBreakdownDto;
import com.outreach.platform.report.model.TimeSeriesDataPoint;
import com.outreach.platform.report.model.TrendDataDto;
import com.outreach.platform.report.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for report aggregation and analytics endpoints.
 * All endpoints are read-only and serve cached analytical data. The shared filter query
 * parameters (dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity) bind
 * straight into {@link ReportQueryParams}.
 */
@RestController
@RequestMapping("/reports")
@Tag(name = "Report Analytics", description = "Aggregation, analytics, KPIs, trends, and comparison endpoints")
public class ReportController {

    private static final String PMO_OR_ADMIN =
            "hasAnyRole('PMO', 'ADMIN', 'TENANT_ADMIN', 'PLATFORM_ADMIN')";

    private final ReportService reportService;

    @Inject
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PreAuthorize(PMO_OR_ADMIN)
    @GetMapping("/by-event")
    public List<EventScoreDto> aggregateByEvent(ReportQueryParams params) {
        return reportService.aggregateByEvent(params);
    }

    @PreAuthorize(PMO_OR_ADMIN)
    @GetMapping("/by-beneficiary")
    public List<BeneficiaryScoreDto> aggregateByBeneficiary(ReportQueryParams params) {
        return reportService.aggregateByBeneficiary(params);
    }

    @PreAuthorize(PMO_OR_ADMIN)
    @GetMapping("/by-city")
    public List<CityScoreDto> aggregateByCity(ReportQueryParams params) {
        return reportService.aggregateByCity(params);
    }

    @PreAuthorize(PMO_OR_ADMIN)
    @GetMapping("/by-poc")
    public List<PocScoreDto> aggregateByPoc(ReportQueryParams params) {
        return reportService.aggregateByPoc(params);
    }

    @GetMapping("/dashboard")
    public DashboardSummaryDto getDashboard(ReportQueryParams params) {
        return reportService.getDashboardSummary(params);
    }

    @GetMapping("/dashboard/trends")
    public TrendDataDto getTrends(ReportQueryParams params) {
        return reportService.getTrends(params);
    }

    @GetMapping("/dashboard/kpis")
    public KpiDto getKpis(ReportQueryParams params) {
        return reportService.getKpis(params);
    }

    @PreAuthorize(PMO_OR_ADMIN)
    @GetMapping("/time-series")
    public List<TimeSeriesDataPoint> getTimeSeries(ReportQueryParams params) {
        return reportService.getTimeSeries(params);
    }

    @PreAuthorize(PMO_OR_ADMIN)
    @GetMapping("/comparison")
    public ComparisonResultDto getComparison(ReportQueryParams params) {
        return reportService.getComparison(params);
    }

    @PreAuthorize(PMO_OR_ADMIN)
    @GetMapping("/heatmap")
    public List<HeatmapEntry> getHeatmap(ReportQueryParams params) {
        return reportService.getHeatmap(params);
    }

    @PreAuthorize(PMO_OR_ADMIN)
    @GetMapping("/sentiment")
    public SentimentBreakdownDto getSentiment(ReportQueryParams params) {
        return reportService.getSentiment(params);
    }

    @PreAuthorize(PMO_OR_ADMIN)
    @GetMapping("/score-distribution")
    public List<ScoreCountDto> getScoreDistribution(ReportQueryParams params) {
        return reportService.getScoreDistribution(params);
    }

    @PreAuthorize(PMO_OR_ADMIN)
    @GetMapping("/participation-rate")
    public ParticipationRateDto getParticipationRate(ReportQueryParams params) {
        return reportService.getParticipationRate(params);
    }

    @PreAuthorize(PMO_OR_ADMIN)
    @GetMapping("/nps")
    public List<NpsResultDto> getNps(ReportQueryParams params) {
        return reportService.getNps(params);
    }
}
