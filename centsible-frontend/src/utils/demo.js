/**
 * The shared demo account: read-only, synthetic data, public credentials by design.
 * The server enforces read-only (any write from this user is rejected with 403) - the UI
 * just explains it. Keep DEMO_EMAIL in sync with DataSeeder.DEMO_EMAIL on the backend.
 */
export const DEMO_EMAIL = 'demo@centsible.com'
export const DEMO_PASSWORD = 'Password123!'

export function isDemoUser(user) {
  return user?.email?.toLowerCase() === DEMO_EMAIL
}
