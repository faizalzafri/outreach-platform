package com.outreach.platform.report.service;

import com.outreach.platform.report.model.DashboardSummaryDto;
import com.outreach.platform.report.model.EventScoreDto;
import com.outreach.platform.report.model.NpsResultDto;
import com.outreach.platform.report.model.ReportQueryParams;
import com.outreach.platform.report.model.SentimentBreakdownDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(jdbcTemplate);
    }

    @Test
    void aggregateByEvent_returnsResults() {
        EventScoreDto expected = new EventScoreDto("e1", "Run", "Mumbai", new BigDecimal("4.25"), 20, 2, 5);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(expected));

        ReportQueryParams params = new ReportQueryParams(null, null, null, null, null, null, null);
        List<EventScoreDto> result = reportService.aggregateByEvent(params);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).eventName()).isEqualTo("Run");
        assertThat(result.get(0).averageScore()).isEqualTo(new BigDecimal("4.25"));
    }

    @Test
    void aggregateByEvent_withFilters_executesSuccessfully() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(Collections.emptyList());

        ReportQueryParams params = new ReportQueryParams(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 6, 30),
                List.of("e1", "e2"),
                List.of("Mumbai"),
                List.of("b1"),
                List.of("p1"),
                "month"
        );
        List<EventScoreDto> result = reportService.aggregateByEvent(params);

        assertThat(result).isEmpty();
    }

    @Test
    void getDashboardSummary_returnsSummary() {
        DashboardSummaryDto expected = new DashboardSummaryDto(50, 30, 200, 500, new BigDecimal("4.00"), 10, 5);
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class)))
                .thenReturn(expected);

        ReportQueryParams params = new ReportQueryParams(null, null, null, null, null, null, null);
        DashboardSummaryDto result = reportService.getDashboardSummary(params);

        assertThat(result.totalEvents()).isEqualTo(50);
        assertThat(result.overallAverageScore()).isEqualTo(new BigDecimal("4.00"));
    }

    @Test
    void getNps_calculatesScoresCorrectly() {
        NpsResultDto expected = new NpsResultDto("e1", "Run", 40, 30, 10, new BigDecimal("37.5"), 80);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(expected));

        ReportQueryParams params = new ReportQueryParams(null, null, null, null, null, null, null);
        List<NpsResultDto> result = reportService.getNps(params);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).npsScore()).isEqualTo(new BigDecimal("37.5"));
    }

    @Test
    void getSentiment_returnsBreakdown() {
        SentimentBreakdownDto expected = new SentimentBreakdownDto(
                60, 25, 15, 100,
                new BigDecimal("60.0"), new BigDecimal("25.0"), new BigDecimal("15.0"));
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(expected);

        ReportQueryParams params = new ReportQueryParams(null, null, null, null, null, null, null);
        SentimentBreakdownDto result = reportService.getSentiment(params);

        assertThat(result.positive()).isEqualTo(60);
        assertThat(result.positivePercentage()).isEqualTo(new BigDecimal("60.0"));
    }

    @Test
    void effectiveGranularity_defaultsToMonth() {
        ReportQueryParams params = new ReportQueryParams(null, null, null, null, null, null, null);
        assertThat(params.effectiveGranularity()).isEqualTo("month");
    }

    @Test
    void effectiveGranularity_respectsExplicitValue() {
        ReportQueryParams params = new ReportQueryParams(null, null, null, null, null, null, "week");
        assertThat(params.effectiveGranularity()).isEqualTo("week");
    }
}
