/**
 * Notifications Content (lazy-loaded)
 *
 * Provides three tabs:
 * - Templates: DataTable + editor form + preview modal
 * - Delivery: DataTable with retry action for failed/bounced
 * - Schedule: Form to create notification schedules
 */

import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { DataTable } from '@/components/data-table/DataTable';
import type { NotificationTemplate, DeliveryRecord, NotificationType } from '@/types/domain';

import styles from './NotificationsContent.module.css';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type TabId = 'templates' | 'delivery' | 'schedule';

interface TemplateFormData {
  name: string;
  type: NotificationType;
  subject: string;
  body: string;
  variables: Array<{ name: string; dataType: string }>;
}

interface PreviewResponse {
  renderedSubject: string;
  renderedBody: string;
}

// ---------------------------------------------------------------------------
// Template Editor
// ---------------------------------------------------------------------------

function TemplateEditor({
  template,
  onClose,
  onSaved,
}: {
  template: NotificationTemplate | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [form, setForm] = useState<TemplateFormData>({
    name: template?.name ?? '',
    type: template?.type ?? 'EMAIL',
    subject: template?.subject ?? '',
    body: template?.body ?? '',
    variables: template?.variables ?? [{ name: '', dataType: 'STRING' }],
  });
  const [errors, setErrors] = useState<Partial<Record<keyof TemplateFormData, string>>>({});

  const validate = (): boolean => {
    const newErrors: Partial<Record<keyof TemplateFormData, string>> = {};
    if (!form.name.trim()) newErrors.name = 'Name is required';
    else if (form.name.length > 100) newErrors.name = 'Max 100 characters';
    if (!form.subject.trim()) newErrors.subject = 'Subject is required';
    else if (form.subject.length > 200) newErrors.subject = 'Max 200 characters';
    if (!form.body.trim()) newErrors.body = 'Body is required';
    else if (form.body.length > 10000) newErrors.body = 'Max 10000 characters';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      const payload = {
        name: form.name.trim(),
        type: form.type,
        subject: form.subject.trim(),
        body: form.body,
        variables: form.variables.filter((v) => v.name.trim()),
      };
      if (template) {
        await httpClient.put(`/notifications/templates/${template.id}`, payload);
      } else {
        await httpClient.post('/notifications/templates', payload);
      }
    },
    onSuccess: () => {
      onSaved();
      onClose();
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validate()) {
      saveMutation.mutate();
    }
  };

  const addVariable = () => {
    setForm((prev) => ({
      ...prev,
      variables: [...prev.variables, { name: '', dataType: 'STRING' }],
    }));
  };

  const removeVariable = (index: number) => {
    setForm((prev) => ({
      ...prev,
      variables: prev.variables.filter((_, i) => i !== index),
    }));
  };

  const updateVariable = (index: number, field: 'name' | 'dataType', value: string) => {
    setForm((prev) => ({
      ...prev,
      variables: prev.variables.map((v, i) => (i === index ? { ...v, [field]: value } : v)),
    }));
  };

  return (
    <div className={styles['editorOverlay']} onClick={onClose} role="dialog" aria-modal="true" aria-label="Template editor">
      <div className={styles['editorPanel']} onClick={(e) => e.stopPropagation()}>
        <h2 className={styles['editorTitle']}>
          {template ? 'Edit Template' : 'Create Template'}
        </h2>

        <form onSubmit={handleSubmit}>
          <div className={styles['formGroup']}>
            <label className={styles['formLabel']} htmlFor="tpl-name">Name</label>
            <input
              id="tpl-name"
              className={styles['formInput']}
              value={form.name}
              onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
              maxLength={100}
            />
            {errors.name && <p className={styles['formError']}>{errors.name}</p>}
          </div>

          <div className={styles['formGroup']}>
            <label className={styles['formLabel']} htmlFor="tpl-type">Type</label>
            <select
              id="tpl-type"
              className={styles['formSelect']}
              value={form.type}
              onChange={(e) => setForm((prev) => ({ ...prev, type: e.target.value as NotificationType }))}
            >
              <option value="EMAIL">Email</option>
              <option value="SMS">SMS</option>
              <option value="PUSH">Push</option>
            </select>
          </div>

          <div className={styles['formGroup']}>
            <label className={styles['formLabel']} htmlFor="tpl-subject">Subject</label>
            <input
              id="tpl-subject"
              className={styles['formInput']}
              value={form.subject}
              onChange={(e) => setForm((prev) => ({ ...prev, subject: e.target.value }))}
              maxLength={200}
            />
            {errors.subject && <p className={styles['formError']}>{errors.subject}</p>}
          </div>

          <div className={styles['formGroup']}>
            <label className={styles['formLabel']} htmlFor="tpl-body">Body</label>
            <textarea
              id="tpl-body"
              className={styles['formTextarea']}
              value={form.body}
              onChange={(e) => setForm((prev) => ({ ...prev, body: e.target.value }))}
              maxLength={10000}
            />
            {errors.body && <p className={styles['formError']}>{errors.body}</p>}
          </div>

          <div className={styles['formGroup']}>
            <label className={styles['formLabel']}>Variables</label>
            <div className={styles['variablesList']}>
              {form.variables.map((variable, index) => (
                <div key={index} className={styles['variableRow']}>
                  <input
                    className={styles['variableInput']}
                    placeholder="Name"
                    value={variable.name}
                    onChange={(e) => updateVariable(index, 'name', e.target.value)}
                    aria-label={`Variable ${index + 1} name`}
                  />
                  <input
                    className={styles['variableInput']}
                    placeholder="Data type"
                    value={variable.dataType}
                    onChange={(e) => updateVariable(index, 'dataType', e.target.value)}
                    aria-label={`Variable ${index + 1} data type`}
                  />
                  <button
                    type="button"
                    className={styles['removeBtn']}
                    onClick={() => removeVariable(index)}
                    aria-label={`Remove variable ${index + 1}`}
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
            <button type="button" className={styles['addBtn']} onClick={addVariable}>
              + Add Variable
            </button>
          </div>

          <div className={styles['formActions']}>
            <button type="button" className={styles['btnSecondary']} onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className={styles['btnPrimary']} disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Saving...' : 'Save'}
            </button>
          </div>

          {saveMutation.isError && (
            <p className={styles['formError']} style={{ marginTop: '0.75rem' }}>
              {(saveMutation.error as { message?: string })?.message ?? 'Failed to save template'}
            </p>
          )}
        </form>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Preview Modal
// ---------------------------------------------------------------------------

function PreviewModal({
  templateId,
  onClose,
}: {
  templateId: string;
  onClose: () => void;
}) {
  const { data, isLoading, isError, error } = useQuery<PreviewResponse>({
    queryKey: ['notifications', 'preview', templateId],
    queryFn: async () => {
      const response = await httpClient.get<PreviewResponse>(
        `/notifications/templates/${templateId}/preview`
      );
      return response.data;
    },
  });

  if (isError) {
    return (
      <div className={styles['previewOverlay']} onClick={onClose} role="dialog" aria-modal="true" aria-label="Preview error">
        <div className={styles['previewPanel']} onClick={(e) => e.stopPropagation()}>
          <h2 className={styles['previewTitle']}>Preview Failed</h2>
          <div className={styles['errorMessage']}>
            {(error as { message?: string })?.message ?? 'Failed to load preview'}
          </div>
          <div className={styles['formActions']}>
            <button type="button" className={styles['btnSecondary']} onClick={onClose}>Close</button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={styles['previewOverlay']} onClick={onClose} role="dialog" aria-modal="true" aria-label="Template preview">
      <div className={styles['previewPanel']} onClick={(e) => e.stopPropagation()}>
        <h2 className={styles['previewTitle']}>Template Preview</h2>
        {isLoading ? (
          <p>Loading preview...</p>
        ) : (
          <>
            {data?.renderedSubject && (
              <div style={{ marginBottom: '1rem' }}>
                <strong>Subject:</strong> {data.renderedSubject}
              </div>
            )}
            <div className={styles['previewContent']}>
              {data?.renderedBody ?? ''}
            </div>
          </>
        )}
        <div className={styles['formActions']}>
          <button type="button" className={styles['btnSecondary']} onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Templates Tab
// ---------------------------------------------------------------------------

function TemplatesTab() {
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<NotificationTemplate | null>(null);
  const [previewId, setPreviewId] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const handleEdit = (template: NotificationTemplate) => {
    setEditingTemplate(template);
    setEditorOpen(true);
  };

  const handleCreate = () => {
    setEditingTemplate(null);
    setEditorOpen(true);
  };

  const handleSaved = () => {
    void queryClient.invalidateQueries({ queryKey: queryKeys.notifications.templates() });
  };

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await httpClient.delete(`/notifications/templates/${id}`);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.notifications.templates() });
    },
  });

  const cloneMutation = useMutation({
    mutationFn: async (id: string) => {
      await httpClient.post(`/notifications/templates/${id}/clone`);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.notifications.templates() });
    },
  });

  const columns: ColumnDef<NotificationTemplate, unknown>[] = useMemo(() => [
    { accessorKey: 'name', header: 'Name' },
    {
      accessorKey: 'type',
      header: 'Type',
      meta: {
        filterType: 'select' as const,
        filterOptions: [
          { label: 'Email', value: 'EMAIL' },
          { label: 'SMS', value: 'SMS' },
          { label: 'Push', value: 'PUSH' },
        ],
      },
    },
    { accessorKey: 'subject', header: 'Subject' },
    { accessorKey: 'engine', header: 'Engine', enableColumnFilter: false },
    { accessorKey: 'status', header: 'Status', enableColumnFilter: false },
    { accessorKey: 'version', header: 'Version', enableColumnFilter: false },
    {
      id: 'actions',
      header: 'Actions',
      enableSorting: false,
      enableColumnFilter: false,
      cell: ({ row }) => {
        const template = row.original;
        return (
          <div>
            <button
              type="button"
              className={styles['actionBtn']}
              onClick={() => handleEdit(template)}
            >
              Edit
            </button>
            <button
              type="button"
              className={styles['actionBtn']}
              onClick={() => setPreviewId(template.id)}
            >
              Preview
            </button>
            <button
              type="button"
              className={styles['actionBtn']}
              onClick={() => cloneMutation.mutate(template.id)}
            >
              Clone
            </button>
            <button
              type="button"
              className={styles['actionBtn']}
              onClick={() => {
                if (window.confirm(`Delete template "${template.name}"?`)) {
                  deleteMutation.mutate(template.id);
                }
              }}
            >
              Delete
            </button>
          </div>
        );
      },
    },
  ], [cloneMutation, deleteMutation]);

  const templateQueryKey = useMemo(() => queryKeys.notifications.templates(), []);

  return (
    <>
      <div style={{ marginBottom: '1rem' }}>
        <button type="button" className={styles['btnPrimary']} onClick={handleCreate}>
          + Create Template
        </button>
      </div>

      <DataTable<NotificationTemplate>
        columns={columns}
        queryKey={templateQueryKey}
        endpoint="/notifications/templates"
        defaultPageSize={10}
        emptyMessage="No notification templates found."
      />

      {editorOpen && (
        <TemplateEditor
          template={editingTemplate}
          onClose={() => setEditorOpen(false)}
          onSaved={handleSaved}
        />
      )}

      {previewId && (
        <PreviewModal
          templateId={previewId}
          onClose={() => setPreviewId(null)}
        />
      )}
    </>
  );
}

// ---------------------------------------------------------------------------
// Delivery Tab
// ---------------------------------------------------------------------------

function DeliveryTab() {
  const queryClient = useQueryClient();

  const retryMutation = useMutation({
    mutationFn: async (eventId: string) => {
      await httpClient.post(`/notifications/retry/${eventId}`);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all });
    },
  });

  const getDeliveryStatusClass = (status: string): string => {
    switch (status) {
      case 'PENDING': return styles['statusPending']!;
      case 'SENT': return styles['statusSent']!;
      case 'DELIVERED': return styles['statusDelivered']!;
      case 'FAILED': return styles['statusFailed']!;
      case 'BOUNCED': return styles['statusBounced']!;
      default: return '';
    }
  };

  const columns: ColumnDef<DeliveryRecord, unknown>[] = useMemo(() => [
    { accessorKey: 'recipient', header: 'Recipient' },
    { accessorKey: 'eventName', header: 'Event' },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ getValue }) => {
        const status = getValue() as string;
        return (
          <span className={`${styles['statusBadge']} ${getDeliveryStatusClass(status)}`}>
            {status}
          </span>
        );
      },
      meta: {
        filterType: 'select' as const,
        filterOptions: [
          { label: 'Pending', value: 'PENDING' },
          { label: 'Sent', value: 'SENT' },
          { label: 'Delivered', value: 'DELIVERED' },
          { label: 'Failed', value: 'FAILED' },
          { label: 'Bounced', value: 'BOUNCED' },
        ],
      },
    },
    {
      accessorKey: 'timestamp',
      header: 'Timestamp',
      cell: ({ getValue }) => {
        const date = new Date(getValue() as string);
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      },
      enableColumnFilter: false,
    },
    {
      id: 'actions',
      header: 'Actions',
      enableSorting: false,
      enableColumnFilter: false,
      cell: ({ row }) => {
        const record = row.original;
        const canRetry = record.status === 'FAILED' || record.status === 'BOUNCED';
        if (!canRetry) return null;
        return (
          <button
            type="button"
            className={styles['retryBtn']}
            onClick={() => retryMutation.mutate(record.id)}
            disabled={retryMutation.isPending}
          >
            Retry
          </button>
        );
      },
    },
  ], [retryMutation]);

  const deliveryQueryKey = useMemo(() => queryKeys.notifications.deliveries({}), []);

  return (
    <DataTable<DeliveryRecord>
      columns={columns}
      queryKey={deliveryQueryKey}
      endpoint="/notifications/deliveries"
      defaultPageSize={10}
      emptyMessage="No delivery records found."
    />
  );
}

// ---------------------------------------------------------------------------
// Schedule Tab
// ---------------------------------------------------------------------------

const CRON_REGEX = /^(\*|[0-9,\-/]+)\s+(\*|[0-9,\-/]+)\s+(\*|[0-9,\-/]+)\s+(\*|[0-9,\-/]+)\s+(\*|[0-9,\-/]+)$/;

function ScheduleTab() {
  const [templateId, setTemplateId] = useState('');
  const [targetEvent, setTargetEvent] = useState('');
  const [triggerType, setTriggerType] = useState<'IMMEDIATE' | 'SCHEDULED' | 'EVENT_LIFECYCLE'>('IMMEDIATE');
  const [cronExpression, setCronExpression] = useState('');
  const [cronError, setCronError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const { data: templates } = useQuery<{ content: NotificationTemplate[] }>({
    queryKey: queryKeys.notifications.templates(),
    queryFn: async () => {
      const response = await httpClient.get<{ content: NotificationTemplate[] }>('/notifications/templates', {
        params: { size: 100 },
      });
      return response.data;
    },
  });

  const scheduleMutation = useMutation({
    mutationFn: async () => {
      const payload: Record<string, unknown> = {
        templateId,
        targetEvent,
        triggerType,
      };
      if (triggerType === 'SCHEDULED') {
        payload.cronExpression = cronExpression;
      }
      await httpClient.post('/notifications/schedules', payload);
    },
    onSuccess: () => {
      setSuccess(true);
      setTemplateId('');
      setTargetEvent('');
      setTriggerType('IMMEDIATE');
      setCronExpression('');
      setTimeout(() => setSuccess(false), 3000);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setCronError(null);

    if (triggerType === 'SCHEDULED') {
      if (!CRON_REGEX.test(cronExpression.trim())) {
        setCronError('Invalid cron expression. Expected 5-field format: * * * * *');
        return;
      }
    }

    scheduleMutation.mutate();
  };

  return (
    <div className={styles['scheduleForm']}>
      <h3 className={styles['scheduleTitle']}>Create Notification Schedule</h3>

      {success && (
        <div className={styles['successMessage']}>Schedule created successfully.</div>
      )}

      <form onSubmit={handleSubmit}>
        <div className={styles['formGroup']}>
          <label className={styles['formLabel']} htmlFor="sched-template">Template</label>
          <select
            id="sched-template"
            className={styles['formSelect']}
            value={templateId}
            onChange={(e) => setTemplateId(e.target.value)}
            required
          >
            <option value="">Select template...</option>
            {templates?.content?.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </select>
        </div>

        <div className={styles['formGroup']}>
          <label className={styles['formLabel']} htmlFor="sched-event">Target Event</label>
          <input
            id="sched-event"
            className={styles['formInput']}
            value={targetEvent}
            onChange={(e) => setTargetEvent(e.target.value)}
            placeholder="Event ID or name"
            required
          />
        </div>

        <div className={styles['formGroup']}>
          <label className={styles['formLabel']} htmlFor="sched-trigger">Trigger Type</label>
          <select
            id="sched-trigger"
            className={styles['formSelect']}
            value={triggerType}
            onChange={(e) => setTriggerType(e.target.value as typeof triggerType)}
          >
            <option value="IMMEDIATE">Immediate</option>
            <option value="SCHEDULED">Scheduled</option>
            <option value="EVENT_LIFECYCLE">Event Lifecycle</option>
          </select>
        </div>

        {triggerType === 'SCHEDULED' && (
          <div className={styles['formGroup']}>
            <label className={styles['formLabel']} htmlFor="sched-cron">Cron Expression</label>
            <input
              id="sched-cron"
              className={styles['formInput']}
              value={cronExpression}
              onChange={(e) => { setCronExpression(e.target.value); setCronError(null); }}
              placeholder="e.g. 0 9 * * 1"
              required
            />
            {cronError && <p className={styles['formError']}>{cronError}</p>}
            <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
              5-field cron: minute hour day-of-month month day-of-week
            </p>
          </div>
        )}

        <div className={styles['formActions']}>
          <button
            type="submit"
            className={styles['btnPrimary']}
            disabled={scheduleMutation.isPending || !templateId || !targetEvent}
          >
            {scheduleMutation.isPending ? 'Creating...' : 'Create Schedule'}
          </button>
        </div>

        {scheduleMutation.isError && (
          <div className={styles['errorMessage']} style={{ marginTop: '0.75rem' }}>
            {(scheduleMutation.error as { message?: string })?.message ?? 'Failed to create schedule'}
          </div>
        )}
      </form>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Main Component
// ---------------------------------------------------------------------------

export function NotificationsContent() {
  const [activeTab, setActiveTab] = useState<TabId>('templates');

  return (
    <div className={styles['container']}>
      <h1 className={styles['pageTitle']}>Notifications</h1>

      {/* Tab Navigation */}
      <div className={styles['tabs']} role="tablist" aria-label="Notification sections">
        <button
          type="button"
          role="tab"
          className={`${styles['tab']} ${activeTab === 'templates' ? styles['tabActive'] : ''}`}
          aria-selected={activeTab === 'templates'}
          onClick={() => setActiveTab('templates')}
        >
          Templates
        </button>
        <button
          type="button"
          role="tab"
          className={`${styles['tab']} ${activeTab === 'delivery' ? styles['tabActive'] : ''}`}
          aria-selected={activeTab === 'delivery'}
          onClick={() => setActiveTab('delivery')}
        >
          Delivery
        </button>
        <button
          type="button"
          role="tab"
          className={`${styles['tab']} ${activeTab === 'schedule' ? styles['tabActive'] : ''}`}
          aria-selected={activeTab === 'schedule'}
          onClick={() => setActiveTab('schedule')}
        >
          Schedule
        </button>
      </div>

      {/* Tab Content */}
      <div role="tabpanel">
        {activeTab === 'templates' && <TemplatesTab />}
        {activeTab === 'delivery' && <DeliveryTab />}
        {activeTab === 'schedule' && <ScheduleTab />}
      </div>
    </div>
  );
}
