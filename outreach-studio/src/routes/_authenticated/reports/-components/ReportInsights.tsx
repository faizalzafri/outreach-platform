/**
 * Insights under the current report filters: participation, sentiment, NPS per event, activity by
 * city, and a comparison of the selected events (or of periods when none are selected).
 */

import { useQuery } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';

import styles from './ReportsContent.module.css';

interface Summary {
  totalBeneficiaries: number;
  activeCities: number;
}
interface Participation {
  totalRegistered: number;
  totalAttended: number;
  participationRate: number;
  feedbackSubmissionRate: number;
}
interface Sentiment {
  positive: number;
  neutral: number;
  negative: number;
  total: number;
}
interface Nps {
  eventId: string;
  eventName: string;
  promoters: number;
  passives: number;
  detractors: number;
  npsScore: number;
  totalResponses: number;
}
interface CityActivity {
  city: string;
  participantCount: number;
  eventCount: number;
}
interface Comparison {
  items: { label: string; averageScore: number | null; feedbackCount: number; volunteerCount: number }[];
}
interface Insights {
  summary: Summary;
  participation: Participation;
  sentiment: Sentiment;
  nps: Nps[];
  cities: CityActivity[];
  comparison: Comparison;
}

export function ReportInsights({ params }: { params: Record<string, unknown> }) {
  const { data, isLoading, isError } = useQuery<Insights>({
    queryKey: ['reports', 'insights', params],
    queryFn: async () => {
      const get = async <T,>(path: string) => (await httpClient.get<T>(path, { params })).data;
      const [summary, participation, sentiment, nps, cities, comparison] = await Promise.all([
        get<Summary>('/reports/dashboard'),
        get<Participation>('/reports/participation-rate'),
        get<Sentiment>('/reports/sentiment'),
        get<Nps[]>('/reports/nps'),
        get<CityActivity[]>('/reports/heatmap'),
        get<Comparison>('/reports/comparison'),
      ]);
      return { summary, participation, sentiment, nps, cities, comparison };
    },
  });

  if (isLoading) return <p>Loading insights...</p>;
  if (isError || !data) return <p role="alert">Insights could not be loaded.</p>;
  const { summary, participation, sentiment, nps, cities, comparison } = data;
  const pct = (n: number) => `${Number(n).toFixed(1)}%`;

  return (
    <section className={styles['chartSection']}>
      <h2 className={styles['chartTitle']}>Insights</h2>
      <dl className={styles['insightTiles']}>
        <div><dt>Attendance</dt><dd>{pct(participation.participationRate)}</dd>
          <dd>{participation.totalAttended} of {participation.totalRegistered} registered</dd></div>
        <div><dt>Feedback given</dt><dd>{pct(participation.feedbackSubmissionRate)}</dd></div>
        <div><dt>Beneficiaries</dt><dd>{summary.totalBeneficiaries}</dd></div>
        <div><dt>Cities</dt><dd>{summary.activeCities}</dd></div>
        <div><dt>Sentiment</dt>
          <dd>{sentiment.total === 0 ? 'Not analysed yet'
            : `${sentiment.positive} positive · ${sentiment.neutral} neutral · ${sentiment.negative} negative`}</dd></div>
      </dl>

      <h3>Net promoter score by event</h3>
      {nps.length === 0 ? <p>No feedback yet.</p> : (
        <table>
          <thead><tr><th scope="col">Event</th><th scope="col">NPS</th><th scope="col">Promoters</th>
            <th scope="col">Passives</th><th scope="col">Detractors</th></tr></thead>
          <tbody>{nps.map((n) => (
            <tr key={n.eventId}><td>{n.eventName}</td><td>{Number(n.npsScore).toFixed(0)}</td>
              <td>{n.promoters}</td><td>{n.passives}</td><td>{n.detractors}</td></tr>
          ))}</tbody>
        </table>
      )}

      <h3>Attendance by city</h3>
      {cities.length === 0 ? <p>No attendance recorded yet.</p> : (
        <table>
          <thead><tr><th scope="col">City</th><th scope="col">Volunteers who attended</th>
            <th scope="col">Events</th></tr></thead>
          <tbody>{cities.map((c) => (
            <tr key={c.city}><td>{c.city}</td><td>{c.participantCount}</td><td>{c.eventCount}</td></tr>
          ))}</tbody>
        </table>
      )}

      <h3>Comparison</h3>
      <p>Select events in the filters to compare them; otherwise periods are compared.</p>
      {comparison.items.length === 0 ? <p>Nothing to compare yet.</p> : (
        <table>
          <thead><tr><th scope="col">Compared</th><th scope="col">Average score</th>
            <th scope="col">Feedback</th><th scope="col">Volunteers</th></tr></thead>
          <tbody>{comparison.items.map((c) => (
            <tr key={c.label}><td>{c.label}</td>
              <td>{c.averageScore == null ? '—' : Number(c.averageScore).toFixed(2)}</td>
              <td>{c.feedbackCount}</td><td>{c.volunteerCount}</td></tr>
          ))}</tbody>
        </table>
      )}
    </section>
  );
}
