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
import com.outreach.platform.report.model.ScoreCountDto;
import com.outreach.platform.report.model.SentimentBreakdownDto;
import com.outreach.platform.report.model.TimeSeriesDataPoint;
import com.outreach.platform.report.model.TrendDataDto;
import com.outreach.platform.common.tenant.TenantContext;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Business logic for report aggregation and analytics queries.
 * Uses JdbcTemplate for read-only native queries against PostgreSQL.
 *
 * <p>Tenant isolation is explicit here: Hibernate's tenant {@code @Filter} only applies to JPA
 * queries, never to raw JDBC, so every query adds its own {@code tenant_id = ?} predicate when a
 * tenant is in context. An empty context (PLATFORM_ADMIN) keeps the cross-tenant view. Cache
 * keys carry the same tenant so one tenant's cached report is never served to another.
 */
@Service
public class ReportService {

    /** SpEL prefix for every cache key; "null" for the cross-tenant PLATFORM_ADMIN view. */
    static final String TENANT_KEY_PREFIX =
            "T(com.outreach.platform.common.tenant.TenantContext).getCurrentTenantId() + ':' + ";

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public ReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'byEvent:' + #params")
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

    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'byBeneficiary:' + #params")
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

    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'byCity:' + #params")
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

    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'byPoc:' + #params")
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

    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'dashboard:' + #params")
    public DashboardSummaryDto getDashboardSummary(ReportQueryParams params) {
        String t = tenantPredicate("");
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM events WHERE %1$s) AS total_events,
                    (SELECT COUNT(*) FROM events WHERE status = 'COMPLETED' AND %1$s) AS completed_events,
                    (SELECT COUNT(*) FROM volunteers WHERE %1$s) AS total_volunteers,
                    (SELECT COUNT(*) FROM volunteer_feedback WHERE %1$s) AS total_feedback,
                    (SELECT AVG(score) FROM volunteer_feedback WHERE %1$s) AS avg_score,
                    (SELECT COUNT(*) FROM beneficiaries WHERE active = true AND %1$s) AS total_beneficiaries,
                    (SELECT COUNT(DISTINCT city) FROM events WHERE city IS NOT NULL AND %1$s) AS active_cities
                """.formatted(t);

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new DashboardSummaryDto(
                rs.getLong("total_events"),
                rs.getLong("completed_events"),
                rs.getLong("total_volunteers"),
                rs.getLong("total_feedback"),
                rs.getBigDecimal("avg_score") != null
                        ? rs.getBigDecimal("avg_score").setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                rs.getLong("total_beneficiaries"),
                rs.getLong("active_cities")
        ), tenantArgs(7));
    }

    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'trends:' + #params")
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
        appendEventScopeFilters(sql, args, params);
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

    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'kpis:' + #params")
    public KpiDto getKpis(ReportQueryParams params) {
        LocalDate startOfMonth = YearMonth.now().atDay(1);

        String sql = """
                SELECT
                    (SELECT AVG(score) FROM volunteer_feedback WHERE %1$s) AS avg_score,
                    (SELECT CASE WHEN COUNT(*) = 0 THEN 0
                            ELSE CAST(COUNT(vf2.id) AS DECIMAL) / COUNT(ee.id) * 100
                            END
                     FROM event_enrollment ee
                     LEFT JOIN volunteer_feedback vf2
                       ON vf2.event_id = ee.event_id AND vf2.volunteer_id = ee.volunteer_id
                     WHERE %2$s
                    ) AS feedback_completion_rate,
                    (SELECT CASE WHEN COUNT(*) = 0 THEN 0
                            ELSE CAST(COUNT(CASE WHEN total_events_participated > 1 THEN 1 END) AS DECIMAL)
                                 / COUNT(*) * 100
                            END
                     FROM volunteers WHERE %1$s
                    ) AS retention_rate,
                    (SELECT CASE WHEN COUNT(*) = 0 THEN 0
                            ELSE AVG(total_events_participated) END
                     FROM volunteers WHERE %1$s
                    ) AS avg_events_per_volunteer,
                    (SELECT COUNT(*) FROM volunteer_feedback
                     WHERE submitted_at >= ? AND %1$s) AS feedback_this_month,
                    (SELECT COUNT(*) FROM events
                     WHERE event_date >= ? AND %1$s) AS events_this_month
                """.formatted(tenantPredicate(""), tenantPredicate("ee."));

        // Bind order follows the placeholders above: four tenant predicates, then each
        // month-start date followed by its subquery's tenant predicate.
        Object monthStart = Date.valueOf(startOfMonth);
        List<Object> args = new ArrayList<>(List.of(tenantArgs(4)));
        args.add(monthStart);
        args.addAll(List.of(tenantArgs(1)));
        args.add(monthStart);
        args.addAll(List.of(tenantArgs(1)));

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
        ), args.toArray());
    }

    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'timeSeries:' + #params")
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

    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'comparison:' + #params")
    public ComparisonResultDto getComparison(ReportQueryParams params) {
        if (params.eventIds() != null && !params.eventIds().isEmpty()) {
            return compareEvents(params);
        }
        return comparePeriods(params);
    }

    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'heatmap:' + #params")
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
        appendEventScopeFilters(sql, args, params);
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

    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'sentiment:' + #params")
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

    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'participation:' + #params")
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
        appendEventScopeFilters(sql, args, params);

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

    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'nps:' + #params")
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

    /** Feedback counts for each score 1–5 (zero where none), leaving out archived feedback. */
    @Cacheable(value = "reportCache", key = TENANT_KEY_PREFIX + "'score-distribution:' + #params")
    public List<ScoreCountDto> getScoreDistribution(ReportQueryParams params) {
        StringBuilder sql = new StringBuilder("""
                SELECT vf.score, COUNT(*) AS responses
                FROM volunteer_feedback vf
                JOIN events e ON e.id = vf.event_id
                WHERE vf.status <> 'ARCHIVED'
                """);
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" GROUP BY vf.score");

        Map<Integer, Long> counts = new HashMap<>();
        jdbcTemplate.query(sql.toString(),
                rs -> { counts.put(rs.getInt("score"), rs.getLong("responses")); }, args.toArray());
        return IntStream.rangeClosed(1, 5)
                .mapToObj(score -> new ScoreCountDto(score, counts.getOrDefault(score, 0L)))
                .toList();
    }

    /** Evicts all report cache entries. */
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
        sql.append(" AND ").append(tenantPredicate("e."));
        args.addAll(List.of(tenantArgs(1)));
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
        appendEventScopeFilters(sql, args, params);
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
        appendEventScopeFilters(sql, args, params);
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

    /**
     * Restricts the {@code e} (events) alias to the caller's tenant and the requested date range.
     * Every other table in these queries is reached through a join on {@code e}, so scoping the
     * event scopes the whole row set.
     */
    private void appendEventScopeFilters(StringBuilder sql, List<Object> args, ReportQueryParams params) {
        sql.append(" AND ").append(tenantPredicate("e."));
        args.addAll(List.of(tenantArgs(1)));
        if (params.dateFrom() != null) {
            sql.append(" AND e.event_date >= ?");
            args.add(Date.valueOf(params.dateFrom()));
        }
        if (params.dateTo() != null) {
            sql.append(" AND e.event_date <= ?");
            args.add(Date.valueOf(params.dateTo()));
        }
    }

    /** {@code <alias>tenant_id = ?} when a tenant is in context, else {@code TRUE} (cross-tenant view). */
    private static String tenantPredicate(String alias) {
        return TenantContext.isPresent() ? alias + "tenant_id = ?" : "TRUE";
    }

    /** The current tenant id once per {@link #tenantPredicate} occurrence; empty without a tenant. */
    private static Object[] tenantArgs(int occurrences) {
        return TenantContext.isPresent()
                ? Collections.nCopies(occurrences, TenantContext.getCurrentTenantId()).toArray()
                : new Object[0];
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
