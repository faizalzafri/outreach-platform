import { describe, it, expect } from 'vitest';
import { sampleVariables } from '../template-preview';

describe('sampleVariables', () => {
  it('fills every placeholder and schema variable, realistically where it can', () => {
    const values = sampleVariables({
      subjectTemplate: 'You are registered for {{eventName}}',
      bodyTemplate: '<p>Hi {{ volunteerName }}, see you on {{eventDate}} ({{shiftLabel}})</p>',
      variablesSchema: JSON.stringify({ type: 'object', properties: { city: { type: 'string' } } }),
    });

    expect(values['eventName']).toBe('Coastal Cleanup Drive');
    expect(values['volunteerName']).toBe('Priya Sharma');
    expect(values['city']).toBe('Mumbai');
    expect(values['shiftLabel']).toBe('[shiftLabel]');
    expect(values['eventDate']).toMatch(/\d{4}/);
  });

  it('copes with a missing or broken schema', () => {
    expect(sampleVariables({ subjectTemplate: null, bodyTemplate: 'Hello', variablesSchema: '{not json' })).toEqual({});
  });
});
