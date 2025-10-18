// Central API helper for the React frontend
const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

/**
 * Get the current authenticated user.
 * Returns JSON: { authenticated, email, name, picture }
 */
export async function getMe() {
  const res = await fetch(`${API_BASE}/api/me`, { credentials: "include" });
  if (!res.ok) throw new Error("Failed to fetch profile");
  return res.json();
}

/**
 * Update user profile info in the backend.
 * payload = { displayName, bio }
 */
export async function updateProfile(payload) {
  const res = await fetch(`${API_BASE}/api/profile`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error("Save failed");
  return res.json();
}

/**
 * Logout and redirect to home with ?logout=1 flag.
 * Backend redirects the session there automatically.
 */
export function logout() {
  window.location.href = `${API_BASE}/logout`;
}
