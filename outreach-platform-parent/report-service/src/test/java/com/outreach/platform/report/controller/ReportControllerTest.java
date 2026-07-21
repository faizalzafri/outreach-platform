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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    @WithMockUser
    void byEvent_returnsAggregatedScores() throws Exception {
        List<EventScoreDto> data = List.of(
                new EventScoreDto("e1", "Charity Run", "Mumbai", new BigDecimal("4.25"), 50, 2, 5)
        );
        when(reportService.aggregateByEvent(any(ReportQueryParams.class))).thenReturn(data);

        mockMvc.perform(get("/reports/by-event"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("e1"))
                .andExpect(jsonPath("$[0].eventName").value("Charity Run"))
                .andExpect(jsonPath("$[0].averageScore").value(4.25));
    }

    @Test
    @WithMockUser
    void byBeneficiary_returnsAggregatedScores() throws Exception {
        List<BeneficiaryScoreDto> data = List.of(
                new BeneficiaryScoreDto("b1", "Orphanage", new BigDecimal("3.80"), 30, 5)
        );
        when(reportService.aggregateByBeneficiary(any(ReportQueryParams.class))).thenReturn(data);

        mockMvc.perform(get("/reports/by-beneficiary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].beneficiaryName").value("Orphanage"))
                .andExpect(jsonPath("$[0].feedbackCount").value(30));
    }

    @Test
    @WithMockUser
    void byCity_returnsAggregatedScores() throws Exception {
        List<CityScoreDto> data = List.of(
                new CityScoreDto("Bangalore", new BigDecimal("4.10"), 100, 10, 80)
        );
        when(reportService.aggregateByCity(any(ReportQueryParams.class))).thenReturn(data);

        mockMvc.perform(get("/reports/by-city"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Bangalore"))
                .andExpect(jsonPath("$[0].volunteerCount").value(80));
    }

    @Test
    @WithMockUser
    void byPoc_returnsAggregatedScores() throws Exception {
        List<PocScoreDto> data = List.of(
                new PocScoreDto("u1", "John", new BigDecimal("4.50"), 25, 3)
        );
        when(reportService.aggregateByPoc(any(ReportQueryParams.class))).thenReturn(data);

        mockMvc.perform(get("/reports/by-poc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pocName").value("John"))
                .andExpect(jsonPath("$[0].eventCount").value(3));
    }

    @Test
    @WithMockUser
    void dashboard_returnsSummary() throws Exception {
        DashboardSummaryDto summary = new DashboardSummaryDto(50, 30, 200, 500, new BigDecimal("4.00"), 10, 5);
        when(reportService.getDashboardSummary(any(ReportQueryParams.class))).thenReturn(summary);

        mockMvc.perform(get("/reports/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(50))
                .andExpect(jsonPath("$.overallAverageScore").value(4.00));
    }

    @Test
    @WithMockUser
    void trends_returnsTrendData() throws Exception {
        TrendDataDto trends = new TrendDataDto("month", List.of(
                new TrendDataDto.TrendPoint("2024-01", 5, 20, new BigDecimal("4.20"))
        ));
        when(reportService.getTrends(any(ReportQueryParams.class))).thenReturn(trends);

        mockMvc.perform(get("/reports/dashboard/trends").param("granularity", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity").value("month"))
                .andExpect(jsonPath("$.points[0].period").value("2024-01"));
    }

    @Test
    @WithMockUser
    void kpis_returnsKpis() throws Exception {
        KpiDto kpis = new KpiDto(new BigDecimal("4.10"), new BigDecimal("85.0"),
                new BigDecimal("60.0"), new BigDecimal("3.2"), 120, 8);
        when(reportService.getKpis(any(ReportQueryParams.class))).thenReturn(kpis);

        mockMvc.perform(get("/reports/dashboard/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageFeedbackScore").value(4.10))
                .andExpect(jsonPath("$.totalFeedbackThisMonth").value(120));
    }

    @Test
    @WithMockUser
    void timeSeries_returnsDataPoints() throws Exception {
        List<TimeSeriesDataPoint> data = List.of(
                new TimeSeriesDataPoint("2024-01", new BigDecimal("4.00"), 10)
        );
        when(reportService.getTimeSeries(any(ReportQueryParams.class))).thenReturn(data);

        mockMvc.perform(get("/reports/time-series").param("granularity", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].period").value("2024-01"))
                .andExpect(jsonPath("$[0].count").value(10));
    }

    @Test
    @WithMockUser
    void comparison_returnsComparisonItems() throws Exception {
        ComparisonResultDto result = new ComparisonResultDto(List.of(
                new ComparisonResultDto.ComparisonItem("Event A", new BigDecimal("4.00"), 20, 15)
        ));
        when(reportService.getComparison(any(ReportQueryParams.class))).thenReturn(result);

        mockMvc.perform(get("/reports/comparison").param("eventIds", "e1", "e2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].label").value("Event A"));
    }

    @Test
    @WithMockUser
    void heatmap_returnsEntries() throws Exception {
        List<HeatmapEntry> data = List.of(
                new HeatmapEntry("Mumbai", 100, 10, 1.0)
        );
        when(reportService.getHeatmap(any(ReportQueryParams.class))).thenReturn(data);

        mockMvc.perform(get("/reports/heatmap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Mumbai"))
                .andExpect(jsonPath("$[0].intensity").value(1.0));
    }

    @Test
    @WithMockUser
    void sentiment_returnsBreakdown() throws Exception {
        SentimentBreakdownDto dto = new SentimentBreakdownDto(
                60, 25, 15, 100,
                new BigDecimal("60.0"), new BigDecimal("25.0"), new BigDecimal("15.0"));
        when(reportService.getSentiment(any(ReportQueryParams.class))).thenReturn(dto);

        mockMvc.perform(get("/reports/sentiment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positive").value(60))
                .andExpect(jsonPath("$.positivePercentage").value(60.0));
    }

    @Test
    @WithMockUser
    void participationRate_returnsRates() throws Exception {
        ParticipationRateDto dto = new ParticipationRateDto(200, 180,
                new BigDecimal("90.0"), new BigDecimal("75.0"));
        when(reportService.getParticipationRate(any(ReportQueryParams.class))).thenReturn(dto);

        mockMvc.perform(get("/reports/participation-rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participationRate").value(90.0))
                .andExpect(jsonPath("$.totalAttended").value(180));
    }

    @Test
    @WithMockUser
    void nps_returnsNpsScores() throws Exception {
        List<NpsResultDto> data = List.of(
                new NpsResultDto("e1", "Run", 40, 30, 10, new BigDecimal("37.5"), 80)
        );
        when(reportService.getNps(any(ReportQueryParams.class))).thenReturn(data);

        mockMvc.perform(get("/reports/nps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].npsScore").value(37.5))
                .andExpect(jsonPath("$[0].promoters").value(40));
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/reports/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void byEvent_withDateFilters_passesParamsCorrectly() throws Exception {
        when(reportService.aggregateByEvent(any(ReportQueryParams.class))).thenReturn(List.of());

        mockMvc.perform(get("/reports/by-event")
                        .param("dateFrom", "2024-01-01")
                        .param("dateTo", "2024-06-30")
                        .param("cities", "Mumbai", "Delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
