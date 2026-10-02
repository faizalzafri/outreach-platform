/**
 * A month of events: each event shows on every day it runs.
 */

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import { httpClient } from '@/lib/http-client';
import type { PageResponse } from '@/types/api';
import type { Event } from '@/types/domain';

import styles from './EventListContent.module.css';

const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const iso = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

export function EventCalendar() {
  const [month, setMonth] = useState(() => new Date(new Date().getFullYear(), new Date().getMonth(), 1));
  const first = iso(month);
  const last = iso(new Date(month.getFullYear(), month.getMonth() + 1, 0));

  // ponytail: one page of 500 events per month is plenty for an outreach calendar
  const { data } = useQuery<PageResponse<Event>>({
    queryKey: ['events', 'calendar', first],
    queryFn: async () =>
      (await httpClient.get<PageResponse<Event>>('/events/calendar', { params: { from: first, to: last, size: 500 } }))
        .data,
  });

  const days: (Date | null)[] = [];
  const lead = (month.getDay() + 6) % 7; // Monday first
  for (let i = 0; i < lead; i++) days.push(null);
  for (let d = 1; d <= new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate(); d++) {
    days.push(new Date(month.getFullYear(), month.getMonth(), d));
  }
  const eventsOn = (day: Date) =>
    (data?.content ?? []).filter((e) => e.eventDate <= iso(day) && iso(day) <= (e.eventEndDate ?? e.eventDate));
  const shift = (by: number) => setMonth(new Date(month.getFullYear(), month.getMonth() + by, 1));

  return (
    <div>
      <div className={styles['calendarHeader']}>
        <button type="button" onClick={() => shift(-1)} aria-label="Previous month">‹</button>
        <h2>{month.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}</h2>
        <button type="button" onClick={() => shift(1)} aria-label="Next month">›</button>
      </div>
      <div className={styles['calendarGrid']} role="grid" aria-label="Events by day">
        {WEEKDAYS.map((w) => (
          <div key={w} className={styles['calendarWeekday']} role="columnheader">{w}</div>
        ))}
        {days.map((day, i) =>
          day ? (
            <div key={i} className={styles['calendarDay']} role="gridcell" aria-label={day.toDateString()}>
              <span className={styles['calendarDate']}>{day.getDate()}</span>
              {eventsOn(day).map((e) => (
                <Link key={e.id} to="/events/$eventId" params={{ eventId: e.id }} search={{ tab: 'overview' }}
                  className={styles['calendarEvent']}>
                  {e.eventName}
                </Link>
              ))}
            </div>
          ) : (
            <div key={i} />
          ),
        )}
      </div>
    </div>
  );
}
