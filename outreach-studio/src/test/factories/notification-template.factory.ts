import type { NotificationTemplate } from '@/types/domain';

let counter = 0;

export function buildNotificationTemplate(overrides: Partial<NotificationTemplate> = {}): NotificationTemplate {
  counter += 1;
  return {
    id: `tmpl-${counter}`,
    name: `Template ${counter}`,
    type: 'EMAIL',
    subject: `Notification Subject ${counter}`,
    body: `Hello {{name}}, this is notification template ${counter}.`,
    engine: 'handlebars',
    status: 'ACTIVE',
    version: 1,
    variables: [{ name: 'name', dataType: 'string' }],
    ...overrides,
  };
}
