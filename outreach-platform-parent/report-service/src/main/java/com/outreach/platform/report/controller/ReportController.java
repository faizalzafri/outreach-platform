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
import com.outreach.platform.report.model.SentimentBreakdownDto;
import com.outreach.platform.report.model.TimeSeriesDataPoint;
import com.outreach.platform.report.model.TrendDataDto;
import com.outreach.platform.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for report aggregation and analytics endpoints.
 * All endpoints are read-only and serve cached analytical data.
 */
@RestController
@RequestMapping("/reports")
@Tag(name = "Report Analytics", description = "Aggregation, analytics, KPIs, trends, and comparison endpoints")
public class ReportController {

    private final ReportService reportService;

    @Inject
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/by-event")
    public List<EventScoreDto> aggregateByEvent(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> eventIds,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> beneficiaryIds,
            @RequestParam(required = false) List<String> pocIds,
            @RequestParam(required = false) String granularity) {
        return reportService.aggregateByEvent(buildParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity));
    }

    @GetMapping("/by-beneficiary")
    public List<BeneficiaryScoreDto> aggregateByBeneficiary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> eventIds,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> beneficiaryIds,
            @RequestParam(required = false) List<String> pocIds,
            @RequestParam(required = false) String granularity) {
        return reportService.aggregateByBeneficiary(buildParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity));
    }

    @GetMapping("/by-city")
    public List<CityScoreDto> aggregateByCity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> eventIds,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> beneficiaryIds,
            @RequestParam(required = false) List<String> pocIds,
            @RequestParam(required = false) String granularity) {
        return reportService.aggregateByCity(buildParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity));
    }

    @GetMapping("/by-poc")
    public List<PocScoreDto> aggregateByPoc(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> eventIds,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> beneficiaryIds,
            @RequestParam(required = false) List<String> pocIds,
            @RequestParam(required = false) String granularity) {
        return reportService.aggregateByPoc(buildParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity));
    }

    @GetMapping("/dashboard")
    public DashboardSummaryDto getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> eventIds,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> beneficiaryIds,
            @RequestParam(required = false) List<String> pocIds,
            @RequestParam(required = false) String granularity) {
        return reportService.getDashboardSummary(buildParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity));
    }

    @GetMapping("/dashboard/trends")
    public TrendDataDto getTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> eventIds,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> beneficiaryIds,
            @RequestParam(required = false) List<String> pocIds,
            @RequestParam(required = false) String granularity) {
        return reportService.getTrends(buildParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity));
    }

    @GetMapping("/dashboard/kpis")
    public KpiDto getKpis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> eventIds,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> beneficiaryIds,
            @RequestParam(required = false) List<String> pocIds,
            @RequestParam(required = false) String granularity) {
        return reportService.getKpis(buildParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity));
    }

    @GetMapping("/time-series")
    public List<TimeSeriesDataPoint> getTimeSeries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> eventIds,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> beneficiaryIds,
            @RequestParam(required = false) List<String> pocIds,
            @RequestParam(required = false) String granularity) {
        return reportService.getTimeSeries(buildParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity));
    }

    @GetMapping("/comparison")
    public ComparisonResultDto getComparison(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> eventIds,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> beneficiaryIds,
            @RequestParam(required = false) List<String> pocIds,
            @RequestParam(required = false) String granularity) {
        return reportService.getComparison(buildParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity));
    }

    @GetMapping("/heatmap")
    public List<HeatmapEntry> getHeatmap(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> eventIds,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> beneficiaryIds,
            @RequestParam(required = false) List<String> pocIds,
            @RequestParam(required = false) String granularity) {
        return reportService.getHeatmap(buildParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity));
    }

    @GetMapping("/sentiment")
    public SentimentBreakdownDto getSentiment(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> eventIds,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> beneficiaryIds,
            @RequestParam(required = false) List<String> pocIds,
            @RequestParam(required = false) String granularity) {
        return reportService.getSentiment(buildParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity));
    }

    @GetMapping("/participation-rate")
    public ParticipationRateDto getParticipationRate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> eventIds,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> beneficiaryIds,
            @RequestParam(required = false) List<String> pocIds,
            @RequestParam(required = false) String granularity) {
        return reportService.getParticipationRate(buildParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity));
    }

    @GetMapping("/nps")
    public List<NpsResultDto> getNps(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> eventIds,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> beneficiaryIds,
            @RequestParam(required = false) List<String> pocIds,
            @RequestParam(required = false) String granularity) {
        return reportService.getNps(buildParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity));
    }

    private ReportQueryParams buildParams(LocalDate dateFrom, LocalDate dateTo,
                                          List<String> eventIds, List<String> cities,
                                          List<String> beneficiaryIds, List<String> pocIds,
                                          String granularity) {
        return new ReportQueryParams(dateFrom, dateTo, eventIds, cities, beneficiaryIds, pocIds, granularity);
    }
}
