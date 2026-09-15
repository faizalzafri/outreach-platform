/**
 * Team Detail Content (lazy-loaded)
 *
 * Displays team name/description/creation date with an inline edit toggle
 * (shared TeamForm component), a paginated member roster, and a searchable
 * "Add Member" selector debounced 300ms with a 2-character minimum.
 */

import { useState } from 'react';
import { useNavigate } from '@tanstack/react-router';

import { TeamForm } from '@/components/teams/TeamForm';
import { useToast } from '@/hooks/useToast';
import {
  useTeamDetail,
  useTeamMembers,
  useUpdateTeam,
  useAddTeamMember,
  useRemoveTeamMember,
  useAvailableUsers,
} from '@/hooks/useTeams';
import type { NormalizedError } from '@/types/api';
import type { TeamFormValues } from '@/lib/zod-schemas';

import { Route } from '../$teamId';
import styles from './TeamDetailContent.module.css';

const MEMBERS_PAGE_SIZE = 20;

export function TeamDetailContent() {
  const { teamId } = Route.useParams();
  const search = Route.useSearch();
  const navigate = useNavigate();
  const { success: toastSuccess, error: toastError } = useToast();

  const [isEditing, setIsEditing] = useState(search.edit);
  const [editError, setEditError] = useState<string | null>(null);
  const [fieldServerErrors, setFieldServerErrors] = useState<Record<string, string>>({});
  const [membersPage, setMembersPage] = useState(0);
  const [addMemberOpen, setAddMemberOpen] = useState(false);
  const [memberSearch, setMemberSearch] = useState('');
  const [addMemberError, setAddMemberError] = useState<string | null>(null);

  const teamQuery = useTeamDetail(teamId);
  const membersQuery = useTeamMembers(teamId, { page: membersPage, size: MEMBERS_PAGE_SIZE });
  const updateTeamMutation = useUpdateTeam(teamId);
  const addMemberMutation = useAddTeamMember(teamId);
  const removeMemberMutation = useRemoveTeamMember(teamId);
  const availableUsersQuery = useAvailableUsers(teamId, memberSearch);

  function handleUpdate(values: TeamFormValues) {
    setEditError(null);
    setFieldServerErrors({});
    updateTeamMutation.mutate(values, {
      onSuccess: () => {
        toastSuccess('Team updated successfully');
        setIsEditing(false);
      },
      onError: (error) => {
        const normalized = error as NormalizedError;
        if (normalized.fieldErrors && normalized.fieldErrors.length > 0) {
          const mapped: Record<string, string> = {};
          for (const fe of normalized.fieldErrors) {
            mapped[fe.field] = fe.message;
          }
          setFieldServerErrors(mapped);
        } else {
          setEditError(normalized.message || 'Failed to update team');
        }
      },
    });
  }

  function handleAddMember(userId: string) {
    setAddMemberError(null);
    addMemberMutation.mutate(userId, {
      onSuccess: () => {
        toastSuccess('Member added');
        setAddMemberOpen(false);
        setMemberSearch('');
      },
      onError: (error) => {
        const normalized = error as NormalizedError;
        setAddMemberError(normalized.message || 'Failed to add member');
      },
    });
  }

  function handleRemoveMember(userId: string) {
    removeMemberMutation.mutate(userId, {
      onSuccess: () => toastSuccess('Member removed'),
      onError: (error) => {
        const normalized = error as NormalizedError;
        toastError(normalized.message || 'Failed to remove member');
      },
    });
  }

  if (teamQuery.isLoading) {
    return (
      <div className={styles['container']}>
        <div className={styles['skeleton']} style={{ height: '2rem', width: '200px' }} />
        <div className={styles['skeletonGrid']}>
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className={styles['skeleton']} style={{ height: '1.5rem' }} />
          ))}
        </div>
      </div>
    );
  }

  if (teamQuery.isError || !teamQuery.data) {
    return (
      <div className={styles['container']}>
        <div className={styles['errorState']}>
          <p className={styles['errorMessage']}>Failed to load team.</p>
          <button type="button" className={styles['retryBtn']} onClick={() => void teamQuery.refetch()}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  const team = teamQuery.data;
  const members = membersQuery.data;
  const totalMemberPages = members?.totalPages ?? 0;

  return (
    <div className={styles['container']}>
      <button
        type="button"
        className={styles['backLink']}
        onClick={() => void navigate({ to: '/teams', search: { page: 1, size: 20 } })}
      >
        ← Back to Teams
      </button>

      {!isEditing && (
        <div className={styles['header']}>
          <div>
            <h1 className={styles['pageTitle']}>{team.name}</h1>
            <p className={styles['description']}>{team.description || 'No description'}</p>
            <p className={styles['meta']}>
              Created {new Date(team.createdDate).toLocaleDateString()} · {team.memberCount} member
              {team.memberCount === 1 ? '' : 's'}
            </p>
          </div>
          <button type="button" className={styles['editBtn']} onClick={() => setIsEditing(true)}>
            Edit
          </button>
        </div>
      )}

      {isEditing && (
        <div className={styles['editSection']}>
          <h2 className={styles['editSectionTitle']}>Edit Team</h2>
          {editError && (
            <div className={styles['serverError']} role="alert">
              {editError}
            </div>
          )}
          <TeamForm
            mode="edit"
            initialValues={{ name: team.name, description: team.description }}
            onSubmit={handleUpdate}
            onCancel={() => setIsEditing(false)}
            isSubmitting={updateTeamMutation.isPending}
            fieldServerErrors={fieldServerErrors}
          />
        </div>
      )}

      <div className={styles['membersSection']}>
        <div className={styles['membersHeader']}>
          <h2 className={styles['membersTitle']}>Members</h2>
          <button
            type="button"
            className={styles['addMemberBtn']}
            onClick={() => setAddMemberOpen((prev) => !prev)}
            aria-expanded={addMemberOpen}
          >
            {addMemberOpen ? 'Cancel' : 'Add Member'}
          </button>
        </div>

        {addMemberOpen && (
          <div className={styles['addMemberPanel']}>
            <input
              type="text"
              className={styles['searchInput']}
              placeholder="Search by username or email (min 2 characters)…"
              value={memberSearch}
              onChange={(e) => setMemberSearch(e.target.value)}
              aria-label="Search available users"
              autoFocus
            />
            {addMemberError && (
              <p className={styles['fieldError']} role="alert">
                {addMemberError}
              </p>
            )}
            {memberSearch.trim().length < 2 && (
              <p className={styles['hint']}>Type at least 2 characters to search.</p>
            )}
            {memberSearch.trim().length >= 2 && availableUsersQuery.isLoading && (
              <p className={styles['hint']}>Searching…</p>
            )}
            {memberSearch.trim().length >= 2 && availableUsersQuery.isError && (
              <p className={styles['fieldError']} role="alert">
                Search failed. Try again.
              </p>
            )}
            {memberSearch.trim().length >= 2 &&
              availableUsersQuery.data &&
              availableUsersQuery.data.length === 0 && <p className={styles['hint']}>No matching users found.</p>}
            {availableUsersQuery.data && availableUsersQuery.data.length > 0 && (
              <ul className={styles['resultsList']} role="listbox">
                {availableUsersQuery.data.map((user) => (
                  <li key={user.id}>
                    <button
                      type="button"
                      role="option"
                      aria-selected="false"
                      className={styles['resultItem']}
                      onClick={() => handleAddMember(user.id)}
                      disabled={addMemberMutation.isPending}
                    >
                      <span className={styles['resultUsername']}>{user.username}</span>
                      <span className={styles['resultEmail']}>{user.email}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        {membersQuery.isLoading && (
          <div className={styles['skeletonGrid']}>
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className={styles['skeleton']} style={{ height: '1.5rem' }} />
            ))}
          </div>
        )}

        {membersQuery.isError && (
          <div className={styles['errorState']}>
            <p className={styles['errorMessage']}>Failed to load members.</p>
            <button type="button" className={styles['retryBtn']} onClick={() => void membersQuery.refetch()}>
              Retry
            </button>
          </div>
        )}

        {!membersQuery.isLoading && !membersQuery.isError && members && (
          <>
            {members.content.length === 0 ? (
              <p className={styles['hint']}>No members yet.</p>
            ) : (
              <table className={styles['table']}>
                <thead>
                  <tr>
                    <th className={styles['th']}>Username</th>
                    <th className={styles['th']}>Email</th>
                    <th className={styles['th']}>Joined</th>
                    <th className={styles['th']}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {members.content.map((member) => (
                    <tr key={member.userId}>
                      <td className={styles['td']}>{member.username ?? member.userId}</td>
                      <td className={styles['td']}>{member.email ?? '—'}</td>
                      <td className={styles['td']}>{new Date(member.joinedAt).toLocaleDateString()}</td>
                      <td className={styles['td']}>
                        <button
                          type="button"
                          className={styles['actionBtnDanger']}
                          onClick={() => handleRemoveMember(member.userId)}
                          disabled={removeMemberMutation.isPending}
                          aria-label={`Remove ${member.username ?? member.userId} from team`}
                        >
                          Remove
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}

            {totalMemberPages > 1 && (
              <div className={styles['paginationControls']}>
                <button
                  type="button"
                  className={styles['paginationBtn']}
                  disabled={membersPage === 0}
                  onClick={() => setMembersPage((p) => p - 1)}
                >
                  Previous
                </button>
                <span className={styles['paginationInfo']}>
                  Page {membersPage + 1} of {totalMemberPages}
                </span>
                <button
                  type="button"
                  className={styles['paginationBtn']}
                  disabled={membersPage + 1 >= totalMemberPages}
                  onClick={() => setMembersPage((p) => p + 1)}
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
