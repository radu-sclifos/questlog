import { Platform } from './platform';

// Groups are ordered by preference; unknown platforms follow alphabetically.
const PREFERRED_PLATFORMS: readonly (readonly string[])[] = [
  ['pc (microsoft windows)', 'pc'],
  ['playstation 5'],
  ['xbox series x|s', 'xbox series x/s', 'xbox series'],
  ['nintendo switch', 'nintendo switch 2'],
  ['playstation 4'],
  ['xbox one', 'xbox 360', 'xbox'],
];

function platformPriority(platform: Platform): number {
  const name = platform.name.trim().toLowerCase();
  const index = PREFERRED_PLATFORMS.findIndex((group) => group.includes(name));
  return index < 0 ? PREFERRED_PLATFORMS.length : index;
}

export function sortPlatforms(platforms: readonly Platform[]): Platform[] {
  return [...platforms].sort((a, b) =>
    platformPriority(a) - platformPriority(b) || a.name.localeCompare(b.name),
  );
}
