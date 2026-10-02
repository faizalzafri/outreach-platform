/**
 * Sample values for previewing a notification template, so the preview reads like a real email
 * instead of showing blanks. Every placeholder in the subject or body ({{name}}) and every
 * variable in the template's schema gets a value: a realistic one for names we know, otherwise
 * the variable's name in brackets so it is obvious what will be filled in.
 */

const KNOWN: Record<string, string> = {
  volunteerName: 'Priya Sharma',
  employeeName: 'Priya Sharma',
  recipientName: 'Priya Sharma',
  name: 'Priya Sharma',
  eventName: 'Coastal Cleanup Drive',
  eventCode: 'EVT-2026-014',
  venue: 'Juhu Beach, North Gate',
  city: 'Mumbai',
  pocName: 'Anita Desai',
  organizationName: 'Default Organization',
  feedbackLink: 'https://outreach.example/feedback/sample',
  eventLink: 'https://outreach.example/events/sample',
};

const PLACEHOLDER = /\{\{\s*([A-Za-z_][\w]*)\s*\}\}/g;

function inAWeek(): string {
  const date = new Date();
  date.setDate(date.getDate() + 7);
  return date.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
}

function schemaVariables(variablesSchema: string | null): string[] {
  if (!variablesSchema) return [];
  try {
    const parsed = JSON.parse(variablesSchema) as { properties?: Record<string, unknown> };
    return Object.keys(parsed.properties ?? {});
  } catch {
    return [];
  }
}

export function sampleVariables(template: {
  subjectTemplate: string | null;
  bodyTemplate: string;
  variablesSchema: string | null;
}): Record<string, string> {
  const names = new Set(schemaVariables(template.variablesSchema));
  for (const text of [template.subjectTemplate ?? '', template.bodyTemplate]) {
    for (const match of text.matchAll(PLACEHOLDER)) {
      names.add(match[1]!);
    }
  }
  const values: Record<string, string> = {};
  for (const name of names) {
    values[name] = /date/i.test(name) ? inAWeek() : KNOWN[name] ?? `[${name}]`;
  }
  return values;
}
