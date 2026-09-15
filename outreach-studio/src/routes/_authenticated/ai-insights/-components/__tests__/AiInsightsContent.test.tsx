import { describe, it, expect } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { axe } from 'jest-axe';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';
import { aiErrorHandlers, MOCK_AI_STATUS } from '@/test/handlers';

async function renderAiInsights() {
  const { AiInsightsContent } = await import('../AiInsightsContent');
  return renderWithProviders(<AiInsightsContent />);
}

describe('AiInsightsContent', () => {
  it('shows the AI service status banner', async () => {
    await renderAiInsights();

    await waitFor(() => {
      expect(screen.getByText('openai')).toBeInTheDocument();
    });
    expect(screen.getByText('UP')).toBeInTheDocument();
  });

  it('submits a summarize job and shows the result once completed', async () => {
    const user = userEvent.setup();
    await renderAiInsights();

    await waitFor(() => {
      expect(screen.getByText('openai')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Feedback text to summarize'), 'Great event, well organized.');
    await user.click(screen.getByRole('button', { name: /^summarize$/i }));

    await waitFor(() => {
      expect(screen.getByText('Mock AI-generated result.')).toBeInTheDocument();
    });
  });

  it('rejects an invalid JSON dataset for anomaly detection', async () => {
    const user = userEvent.setup();
    await renderAiInsights();

    await waitFor(() => {
      expect(screen.getByText('openai')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Dataset (JSON array)'), 'not json');
    await user.click(screen.getByRole('button', { name: /detect anomalies/i }));

    await waitFor(() => {
      expect(screen.getByText(/enter a valid json array/i)).toBeInTheDocument();
    });
  });

  it('submits a question and shows the answer once completed', async () => {
    const user = userEvent.setup();
    await renderAiInsights();

    await waitFor(() => {
      expect(screen.getByText('openai')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Question'), 'What is the average score?');
    await user.click(screen.getByRole('button', { name: /^ask$/i }));

    await waitFor(() => {
      expect(screen.getByText('Mock AI-generated result.')).toBeInTheDocument();
    });
  });

  it('shows a failed-job message', async () => {
    server.use(aiErrorHandlers.jobFailed);

    const user = userEvent.setup();
    await renderAiInsights();

    await waitFor(() => {
      expect(screen.getByText('openai')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Feedback text to summarize'), 'Some feedback text.');
    await user.click(screen.getByRole('button', { name: /^summarize$/i }));

    await waitFor(() => {
      expect(screen.getByText('The AI provider returned an error.')).toBeInTheDocument();
    });
  });

  it('disables a panel and shows a notice when the feature is disabled', async () => {
    server.use(
      http.get('/api/ai/status', () => {
        return HttpResponse.json({
          ...MOCK_AI_STATUS,
          features: { ...MOCK_AI_STATUS.features, summarize: false },
        });
      }),
    );

    await renderAiInsights();

    await waitFor(() => {
      expect(screen.getByLabelText('Feedback text to summarize')).toBeDisabled();
    });
    expect(screen.getAllByText('This feature is currently disabled.')[0]).toBeInTheDocument();
  });

  describe('accessibility', () => {
    it('has no axe violations once loaded', async () => {
      const { container } = await renderAiInsights();

      await waitFor(() => {
        expect(screen.getByText('openai')).toBeInTheDocument();
      });

      expect(await axe(container)).toHaveNoViolations();
    });
  });
});
