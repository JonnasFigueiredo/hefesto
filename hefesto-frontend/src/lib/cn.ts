/**
 * Tiny classnames helper. Joins truthy class values with spaces.
 */
export function cn(...args: Array<string | false | null | undefined>): string {
  return args.filter(Boolean).join(' ')
}
