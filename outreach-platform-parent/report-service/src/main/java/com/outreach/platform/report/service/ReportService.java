package com.outreach.platform.report.service;

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
import jakarta.inject.Inject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for report aggregation and analytics queries.
 * Uses JdbcTemplate for read-only native queries against PostgreSQL.
 */
@Service
public class ReportService {

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public ReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Cacheable(value = "reportCache", key = "'byEvent:' + #params.hashCode()")
    public List<EventScoreDto> aggregateByEvent(ReportQueryParams params) {
        StringBuilder sql = new StringBuilder("""
                SELECT e.id::text AS event_id, e.event_name, e.city,
                       AVG(vf.score) AS avg_score,
                       COUNT(vf.id) AS feedback_count,
                       MIN(vf.score) AS min_score,
                       MAX(vf.score) AS max_score
                FROM volunteer_feedback vf
                JOIN events e ON e.id = vf.event_id
                WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" GROUP BY e.id, e.event_name, e.city ORDER BY avg_score DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new EventScoreDto(
                rs.getString("event_id"),
                rs.getString("event_name"),
                rs.getString("city"),
                rs.getBigDecimal("avg_score") != null
                        ? rs.getBigDecimal("avg_score").setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                rs.getLong("feedback_count"),
                rs.getInt("min_score"),
                rs.getInt("max_score")
        ), args.toArray());
    }

    @Cacheable(value = "reportCache", key = "'byBeneficiary:' + #params.hashCode()")
    public List<BeneficiaryScoreDto> aggregateByBeneficiary(ReportQueryParams params) {
        StringBuilder sql = new StringBuilder("""
                SELECT b.id::text AS beneficiary_id, b.name AS beneficiary_name,
                       AVG(vf.score) AS avg_score,
                       COUNT(vf.id) AS feedback_count,
                       COUNT(DISTINCT e.id) AS event_count
                FROM volunteer_feedback vf
                JOIN events e ON e.id = vf.event_id
                JOIN event_beneficiary eb ON eb.event_id = e.id
                JOIN beneficiaries b ON b.id = eb.beneficiary_id
                WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" GROUP BY b.id, b.name ORDER BY avg_score DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new BeneficiaryScoreDto(
                rs.getString("beneficiary_id"),
                rs.getString("beneficiary_name"),
                rs.getBigDecimal("avg_score") != null
                        ? rs.getBigDecimal("avg_score").setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                rs.getLong("feedback_count"),
                rs.getLong("event_count")
        ), args.toArray());
    }

    @Cacheable(value = "reportCache", key = "'byCity:' + #params.hashCode()")
    public List<CityScoreDto> aggregateByCity(ReportQueryParams params) {
        StringBuilder sql = new StringBuilder("""
                SELECT e.city,
                       AVG(vf.score) AS avg_score,
                       COUNT(vf.id) AS feedback_count,
                       COUNT(DISTINCT e.id) AS event_count,
                       COUNT(DISTINCT vf.volunteer_id) AS volunteer_count
                FROM volunteer_feedback vf
                JOIN events e ON e.id = vf.event_id
                WHERE e.city IS NOT NULL
                """);
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" GROUP BY e.city ORDER BY avg_score DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new CityScoreDto(
                rs.getString("city"),
                rs.getBigDecimal("avg_score") != null
                        ? rs.getBigDecimal("avg_score").setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                rs.getLong("feedback_count"),
                rs.getLong("event_count"),
                rs.getLong("volunteer_count")
        ), args.toArray());
    }

    @Cacheable(value = "reportCache", key = "'byPoc:' + #params.hashCode()")
    public List<PocScoreDto> aggregateByPoc(ReportQueryParams params) {
        StringBuilder sql = new StringBuilder("""
                SELECT u.id::text AS poc_id, u.username AS poc_name,
                       AVG(vf.score) AS avg_score,
                       COUNT(vf.id) AS feedback_count,
                       COUNT(DISTINCT e.id) AS event_count
                FROM volunteer_feedback vf
                JOIN events e ON e.id = vf.event_id
                JOIN poc_assignments pa ON pa.event_id = e.id
                JOIN users u ON u.id = pa.user_id
                WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" GROUP BY u.id, u.username ORDER BY avg_score DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new PocScoreDto(
                rs.getString("poc_id"),
                rs.getString("poc_name"),
                rs.getBigDecimal("avg_score") != null
                        ? rs.getBigDecimal("avg_score").setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                rs.getLong("feedback_count"),
                rs.getLong("event_count")
        ), args.toArray());
    }

    @Cacheable(value = "reportCache", key = "'dashboard:' + #params.hashCode()")
    public DashboardSummaryDto getDashboardSummary(ReportQueryParams params) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    (SELECT COUNT(*) FROM events WHERE 1=1) AS total_events,
                    (SELECT COUNT(*) FROM events WHERE status = 'COMPLETED') AS completed_events,
                    (SELECT COUNT(*) FROM volunteers) AS total_volunteers,
                    (SELECT COUNT(*) FROM volunteer_feedback) AS total_feedback,
                    (SELECT AVG(score) FROM volunteer_feedback) AS avg_score,
                    (SELECT COUNT(*) FROM beneficiaries WHERE active = true) AS total_beneficiaries,
                    (SELECT COUNT(DISTINCT city) FROM events WHERE city IS NOT NULL) AS active_cities
                """);

        return jdbcTemplate.queryForObject(sql.toString(), (rs, rowNum) -> new DashboardSummaryDto(
                rs.getLong("total_events"),
                rs.getLong("completed_events"),
                rs.getLong("total_volunteers"),
                rs.getLong("total_feedback"),
                rs.getBigDecimal("avg_score") != null
                        ? rs.getBigDecimal("avg_score").setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                rs.getLong("total_beneficiaries"),
                rs.getLong("active_cities")
        ));
    }

    @Cacheable(value = "reportCache", key = "'trends:' + #params.hashCode()")
    public TrendDataDto getTrends(ReportQueryParams params) {
        String granularity = params.effectiveGranularity();
        String truncExpr = mapGranularityToTrunc(granularity);

        StringBuilder sql = new StringBuilder(String.format("""
                SELECT DATE_TRUNC('%s', e.event_date) AS period,
                       COUNT(DISTINCT e.id) AS event_count,
                       COUNT(vf.id) AS feedback_count,
                       AVG(vf.score) AS avg_score
                FROM events e
                LEFT JOIN volunteer_feedback vf ON vf.event_id = e.id
                WHERE e.event_date IS NOT NULL
                """, truncExpr));
        List<Object> args = new ArrayList<>();
        appendDateFilters(sql, args, params);
        sql.append(String.format(" GROUP BY DATE_TRUNC('%s', e.event_date) ORDER BY period", truncExpr));

        List<TrendDataDto.TrendPoint> points = jdbcTemplate.query(sql.toString(), (rs, rowNum) ->
                new TrendDataDto.TrendPoint(
                        rs.getString("period"),
                        rs.getLong("event_count"),
                        rs.getLong("feedback_count"),
                        rs.getBigDecimal("avg_score") != null
                                ? rs.getBigDecimal("avg_score").setScale(2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO
                ), args.toArray());

        return new TrendDataDto(granularity, points);
    }

    @Cacheable(value = "reportCache", key = "'kpis:' + #params.hashCode()")
    public KpiDto getKpis(ReportQueryParams params) {
        LocalDate startOfMonth = YearMonth.now().atDay(1);

        String sql = """
                SELECT
                    (SELECT AVG(score) FROM volunteer_feedback) AS avg_score,
                    (SELECT CASE WHEN COUNT(*) = 0 THEN 0
                            ELSE CAST(COUNT(vf2.id) AS DECIMAL) / COUNT(ee.id) * 100
                            END
                     FROM event_enrollment ee
                     LEFT JOIN volunteer_feedback vf2
                       ON vf2.event_id = ee.event_id AND vf2.volunteer_id = ee.volunteer_id
                    ) AS feedback_completion_rate,
                    (SELECT CASE WHEN COUNT(*) = 0 THEN 0
                            ELSE CAST(COUNT(CASE WHEN total_events_participated > 1 THEN 1 END) AS DECIMAL)
                                 / COUNT(*) * 100
                            END
                     FROM volunteers
                    ) AS retention_rate,
                    (SELECT CASE WHEN COUNT(*) = 0 THEN 0
                            ELSE AVG(total_events_participated) END
                     FROM volunteers
                    ) AS avg_events_per_volunteer,
                    (SELECT COUNT(*) FROM volunteer_feedback
                     WHERE submitted_at >= ?) AS feedback_this_month,
                    (SELECT COUNT(*) FROM events
                     WHERE event_date >= ?) AS events_this_month
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new KpiDto(
                rs.getBigDecimal("avg_score") != null
                        ? rs.getBigDecimal("avg_score").setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                rs.getBigDecimal("feedback_completion_rate") != null
                        ? rs.getBigDecimal("feedback_completion_rate").setScale(1, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                rs.getBigDecimal("retention_rate") != null
                        ? rs.getBigDecimal("retention_rate").setScale(1, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                rs.getBigDecimal("avg_events_per_volunteer") != null
                        ? rs.getBigDecimal("avg_events_per_volunteer").setScale(1, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                rs.getLong("feedback_this_month"),
                rs.getLong("events_this_month")
        ), Date.valueOf(startOfMonth), Date.valueOf(startOfMonth));
    }

    @Cacheable(value = "reportCache", key = "'timeSeries:' + #params.hashCode()")
    public List<TimeSeriesDataPoint> getTimeSeries(ReportQueryParams params) {
        String granularity = params.effectiveGranularity();
        String truncExpr = mapGranularityToTrunc(granularity);

        StringBuilder sql = new StringBuilder(String.format("""
                SELECT DATE_TRUNC('%s', vf.submitted_at) AS period,
                       AVG(vf.score) AS avg_score,
                       COUNT(vf.id) AS feedback_count
                FROM volunteer_feedback vf
                JOIN events e ON e.id = vf.event_id
                WHERE vf.submitted_at IS NOT NULL
                """, truncExpr));
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(String.format(" GROUP BY DATE_TRUNC('%s', vf.submitted_at) ORDER BY period", truncExpr));

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new TimeSeriesDataPoint(
                rs.getString("period"),
                rs.getBigDecimal("avg_score") != null
                        ? rs.getBigDecimal("avg_score").setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                rs.getLong("feedback_count")
        ), args.toArray());
    }

    @Cacheable(value = "reportCache", key = "'comparison:' + #params.hashCode()")
    public ComparisonResultDto getComparison(ReportQueryParams params) {
        if (params.eventIds() != null && !params.eventIds().isEmpty()) {
            return compareEvents(params);
        }
        return comparePeriods(params);
    }

    @Cacheable(value = "reportCache", key = "'heatmap:' + #params.hashCode()")
    public List<HeatmapEntry> getHeatmap(ReportQueryParams params) {
        StringBuilder sql = new StringBuilder("""
                SELECT e.city,
                       COUNT(DISTINCT ee.volunteer_id) AS participant_count,
                       COUNT(DISTINCT e.id) AS event_count
                FROM events e
                JOIN event_enrollment ee ON ee.event_id = e.id
                WHERE e.city IS NOT NULL AND ee.attendance_status = 'ATTENDED'
                """);
        List<Object> args = new ArrayList<>();
        appendDateFilters(sql, args, params);
        if (params.cities() != null && !params.cities().isEmpty()) {
            sql.append(" AND e.city = ANY(?)");
            args.add(params.cities().toArray(new String[0]));
        }
        sql.append(" GROUP BY e.city ORDER BY participant_count DESC");

        List<HeatmapEntry> entries = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            long participants = rs.getLong("participant_count");
            long events = rs.getLong("event_count");
            return new HeatmapEntry(
                    rs.getString("city"),
                    participants,
                    events,
                    0.0
            );
        }, args.toArray());

        if (entries.isEmpty()) {
            return entries;
        }

        long maxParticipants = entries.stream()
                .mapToLong(HeatmapEntry::participantCount)
                .max().orElse(1);

        return entries.stream()
                .map(e -> new HeatmapEntry(
                        e.city(),
                        e.participantCount(),
                        e.eventCount(),
                        maxParticipants > 0 ? (double) e.participantCount() / maxParticipants : 0.0
                ))
                .toList();
    }

    @Cacheable(value = "reportCache", key = "'sentiment:' + #params.hashCode()")
    public SentimentBreakdownDto getSentiment(ReportQueryParams params) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    COUNT(CASE WHEN vf.sentiment = 'POSITIVE' THEN 1 END) AS positive,
                    COUNT(CASE WHEN vf.sentiment = 'NEUTRAL' THEN 1 END) AS neutral,
                    COUNT(CASE WHEN vf.sentiment = 'NEGATIVE' THEN 1 END) AS negative,
                    COUNT(*) AS total
                FROM volunteer_feedback vf
                JOIN events e ON e.id = vf.event_id
                WHERE vf.sentiment IS NOT NULL
                """);
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);

        return jdbcTemplate.queryForObject(sql.toString(), (rs, rowNum) -> {
            long positive = rs.getLong("positive");
            long neutral = rs.getLong("neutral");
            long negative = rs.getLong("negative");
            long total = rs.getLong("total");

            return new SentimentBreakdownDto(
                    positive, neutral, negative, total,
                    total > 0 ? BigDecimal.valueOf(positive * 100.0 / total).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                    total > 0 ? BigDecimal.valueOf(neutral * 100.0 / total).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                    total > 0 ? BigDecimal.valueOf(negative * 100.0 / total).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO
            );
        }, args.toArray());
    }

    @Cacheable(value = "reportCache", key = "'participation:' + #params.hashCode()")
    public ParticipationRateDto getParticipationRate(ReportQueryParams params) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    COUNT(*) AS total_registered,
                    COUNT(CASE WHEN ee.attendance_status = 'ATTENDED' THEN 1 END) AS total_attended,
                    COUNT(DISTINCT vf.id) AS total_feedback
                FROM event_enrollment ee
                JOIN events e ON e.id = ee.event_id
                LEFT JOIN volunteer_feedback vf ON vf.event_id = ee.event_id AND vf.volunteer_id = ee.volunteer_id
                WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        appendDateFilters(sql, args, params);

        return jdbcTemplate.queryForObject(sql.toString(), (rs, rowNum) -> {
            long registered = rs.getLong("total_registered");
            long attended = rs.getLong("total_attended");
            long feedback = rs.getLong("total_feedback");

            BigDecimal participationRate = registered > 0
                    ? BigDecimal.valueOf(attended * 100.0 / registered).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal feedbackRate = attended > 0
                    ? BigDecimal.valueOf(feedback * 100.0 / attended).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            return new ParticipationRateDto(registered, attended, participationRate, feedbackRate);
        }, args.toArray());
    }

    @Cacheable(value = "reportCache", key = "'nps:' + #params.hashCode()")
    public List<NpsResultDto> getNps(ReportQueryParams params) {
        StringBuilder sql = new StringBuilder("""
                SELECT e.id::text AS event_id, e.event_name,
                       COUNT(CASE WHEN vf.score >= 4 THEN 1 END) AS promoters,
                       COUNT(CASE WHEN vf.score = 3 THEN 1 END) AS passives,
                       COUNT(CASE WHEN vf.score <= 2 THEN 1 END) AS detractors,
                       COUNT(vf.id) AS total_responses
                FROM volunteer_feedback vf
                JOIN events e ON e.id = vf.event_id
                WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" GROUP BY e.id, e.event_name ORDER BY e.event_name");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            long promoters = rs.getLong("promoters");
            long detractors = rs.getLong("detractors");
            long total = rs.getLong("total_responses");
            long passives = rs.getLong("passives");

            BigDecimal nps = total > 0
                    ? BigDecimal.valueOf((promoters - detractors) * 100.0 / total).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            return new NpsResultDto(
                    rs.getString("event_id"),
                    rs.getString("event_name"),
                    promoters, passives, detractors, nps, total
            );
        }, args.toArray());
    }

    /**
     * Evicts all report cache entries. Called when ImportJobCompleted events are received.
     */
    @CacheEvict(value = "reportCache", allEntries = true)
    public void evictAllCaches() {
        // Cache eviction handled by Spring annotation
    }

    // --- Private helper methods ---

    private ComparisonResultDto compareEvents(ReportQueryParams params) {
        StringBuilder sql = new StringBuilder("""
                SELECT e.event_name AS label,
                       AVG(vf.score) AS avg_score,
                       COUNT(vf.id) AS feedback_count,
                       COUNT(DISTINCT vf.volunteer_id) AS volunteer_count
                FROM volunteer_feedback vf
                JOIN events e ON e.id = vf.event_id
                WHERE e.id::text = ANY(?)
                """);
        List<Object> args = new ArrayList<>();
        args.add(params.eventIds().toArray(new String[0]));
        sql.append(" GROUP BY e.id, e.event_name ORDER BY e.event_name");

        List<ComparisonResultDto.ComparisonItem> items = jdbcTemplate.query(sql.toString(), (rs, rowNum) ->
                new ComparisonResultDto.ComparisonItem(
                        rs.getString("label"),
                        rs.getBigDecimal("avg_score") != null
                                ? rs.getBigDecimal("avg_score").setScale(2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO,
                        rs.getLong("feedback_count"),
                        rs.getLong("volunteer_count")
                ), args.toArray());

        return new ComparisonResultDto(items);
    }

    private ComparisonResultDto comparePeriods(ReportQueryParams params) {
        String granularity = params.effectiveGranularity();
        String truncExpr = mapGranularityToTrunc(granularity);

        StringBuilder sql = new StringBuilder(String.format("""
                SELECT DATE_TRUNC('%s', e.event_date)::text AS label,
                       AVG(vf.score) AS avg_score,
                       COUNT(vf.id) AS feedback_count,
                       COUNT(DISTINCT vf.volunteer_id) AS volunteer_count
                FROM volunteer_feedback vf
                JOIN events e ON e.id = vf.event_id
                WHERE e.event_date IS NOT NULL
                """, truncExpr));
        List<Object> args = new ArrayList<>();
        appendDateFilters(sql, args, params);
        sql.append(String.format(" GROUP BY DATE_TRUNC('%s', e.event_date) ORDER BY label", truncExpr));

        List<ComparisonResultDto.ComparisonItem> items = jdbcTemplate.query(sql.toString(), (rs, rowNum) ->
                new ComparisonResultDto.ComparisonItem(
                        rs.getString("label"),
                        rs.getBigDecimal("avg_score") != null
                                ? rs.getBigDecimal("avg_score").setScale(2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO,
                        rs.getLong("feedback_count"),
                        rs.getLong("volunteer_count")
                ), args.toArray());

        return new ComparisonResultDto(items);
    }

    private void appendFilters(StringBuilder sql, List<Object> args, ReportQueryParams params) {
        appendDateFilters(sql, args, params);
        if (params.eventIds() != null && !params.eventIds().isEmpty()) {
            sql.append(" AND e.id::text = ANY(?)");
            args.add(params.eventIds().toArray(new String[0]));
        }
        if (params.cities() != null && !params.cities().isEmpty()) {
            sql.append(" AND e.city = ANY(?)");
            args.add(params.cities().toArray(new String[0]));
        }
        if (params.beneficiaryIds() != null && !params.beneficiaryIds().isEmpty()) {
            sql.append(" AND e.id IN (SELECT eb2.event_id FROM event_beneficiary eb2 WHERE eb2.beneficiary_id::text = ANY(?))");
            args.add(params.beneficiaryIds().toArray(new String[0]));
        }
        if (params.pocIds() != null && !params.pocIds().isEmpty()) {
            sql.append(" AND e.id IN (SELECT pa2.event_id FROM poc_assignments pa2 WHERE pa2.user_id::text = ANY(?))");
            args.add(params.pocIds().toArray(new String[0]));
        }
    }

    private void appendDateFilters(StringBuilder sql, List<Object> args, ReportQueryParams params) {
        if (params.dateFrom() != null) {
            sql.append(" AND e.event_date >= ?");
            args.add(Date.valueOf(params.dateFrom()));
        }
        if (params.dateTo() != null) {
            sql.append(" AND e.event_date <= ?");
            args.add(Date.valueOf(params.dateTo()));
        }
    }

    private String mapGranularityToTrunc(String granularity) {
        return switch (granularity) {
            case "day" -> "day";
            case "week" -> "week";
            case "quarter" -> "quarter";
            default -> "month";
        };
    }
}
