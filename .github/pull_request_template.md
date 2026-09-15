## Summary

<!-- What changed and why. -->

## Test plan

<!-- How you verified this: commands run, scenarios covered. -->

## Frontend checklist (skip if this PR doesn't touch `outreach-studio/`)

- [ ] Accessibility assertions present for any new/changed page or list view (`jest-axe` — see `src/routes/_authenticated/events/-components/__tests__/EventPages.test.tsx` for the pattern), tracked separately from the coverage threshold
- [ ] No hardcoded colors — all styling uses tokens from `src/styles/theme.css`
- [ ] New tenant-scoped queries include `tenantId` in their query key
