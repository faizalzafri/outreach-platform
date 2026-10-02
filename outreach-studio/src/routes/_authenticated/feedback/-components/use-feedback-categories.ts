import { useQuery } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';

/** Shown until (or if) the categories endpoint answers; mirrors feedback-service's list. */
const FALLBACK_CATEGORIES = [
  'Communication',
  'Organization',
  'Content',
  'Logistics',
  'Teamwork',
  'Leadership',
  'Impact',
  'Safety',
  'Overall',
];

/** The feedback categories, from feedback-service, shared by the form and the list filter. */
export function useFeedbackCategories(): string[] {
  const { data } = useQuery<string[]>({
    queryKey: [...queryKeys.feedback.all, 'categories'],
    queryFn: async () => (await httpClient.get<string[]>('/feedback/categories')).data,
    staleTime: 5 * 60 * 1000,
    placeholderData: FALLBACK_CATEGORIES,
  });
  return data ?? FALLBACK_CATEGORIES;
}
