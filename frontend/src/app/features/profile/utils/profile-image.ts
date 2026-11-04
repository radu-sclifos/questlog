export function avatarUrl(username: string, revision: string | null): string | null {
  return revision
    ? `/api/users/${encodeURIComponent(username)}/avatar?v=${encodeURIComponent(revision)}`
    : null;
}

export function initials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => Array.from(part)[0] ?? '')
    .join('')
    .toUpperCase();
}
