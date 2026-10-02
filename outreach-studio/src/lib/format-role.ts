/** ROLE_PMO → "PMO", ROLE_ADMIN → "Admin", ROLE_PLATFORM_ADMIN → "Platform admin" */
export function formatRole(role: string): string {
  const name = role.replace(/^ROLE_/, '');
  if (name.length <= 3) return name; // acronyms such as PMO and POC
  const words = name.toLowerCase().replace(/_/g, ' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}
