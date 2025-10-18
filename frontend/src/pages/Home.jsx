import { useEffect, useState } from "react";
import { getMe } from "../lib/api";

const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

export default function Home() {
  const [me, setMe] = useState(null);
  const [loading, setLoading] = useState(true);
  const [justLoggedOut, setJustLoggedOut] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get("logout") === "1") setJustLoggedOut(true);

    getMe()
      .then((data) => setMe(data))
      .catch(() => setMe(null))
      .finally(() => setLoading(false));
  }, []);

  const loginGoogle = () =>
    (window.location.href = `${API_BASE}/oauth2/authorization/google`);
  const loginGithub = () =>
    (window.location.href = `${API_BASE}/oauth2/authorization/github`);

  return (
    <div className="page">
      <div className="card">
        <span className="eyebrow">Secure OAuth 2.0</span>
        <h1 className="title">Welcome</h1>
        <p className="muted">Sign in using a provider below to continue.</p>

        {justLoggedOut && (
          <div className="notice success">Logged out successfully.</div>
        )}

        {loading ? (
          <div className="hint">Checking session…</div>
        ) : me?.authenticated ? (
          <div className="notice success">
            Logged in as <strong>{me.email}</strong>. Go to your{" "}
            <a href="/profile">profile</a>.
          </div>
        ) : (
          <div className="providers">
            <button className="btn light" onClick={loginGoogle}>
              <img src="/google.svg" alt="Google" />
              Continue with Google
            </button>
            <button className="btn dark" onClick={loginGithub}>
              <img src="/github.svg" alt="GitHub" />
              Continue with GitHub
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
