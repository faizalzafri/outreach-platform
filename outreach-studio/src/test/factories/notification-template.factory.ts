import type { NotificationTemplate } from '@/types/domain';

let counter = 0;

export function buildNotificationTemplate(overrides: Partial<NotificationTemplate> = {}): NotificationTemplate {
  counter += 1;
  return {
    id: `tmpl-${counter}`,
    name: `Template ${counter}`,
    type: 'EMAIL',
    subjectTemplate: `Notification Subject ${counter}`,
    bodyTemplate: `Hello {{name}}, this is notification template ${counter}.`,
    engine: 'THYMELEAF',
    active: true,
    version: 1,
    variablesSchema: JSON.stringify({ type: 'object', properties: { name: { type: 'string' } } }),
    ...overrides,
  };
}
