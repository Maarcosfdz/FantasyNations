/**
 * Returns initials from a player name for use as avatar fallback.
 */
export function getInitials(name: string): string {
  return name
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

/**
 * Generates a deterministic background color from a string.
 */
export function getAvatarColor(seed: string): string {
  const colors = [
    "#1a56db", "#0e9f6e", "#e3a008", "#e74694", "#9061f9",
    "#3f83f8", "#31c48d", "#f05252", "#ff8a4c", "#8da2fb",
  ];
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = seed.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
}
