import { useEffect, useState } from "react";
import { getMe, logout } from "../lib/api";

const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

export default function Navbar() {
  const [me, setMe] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const m = await getMe();
        setMe(m?.authenticated ? m : null);
      } catch {
        setMe(null);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  return (
    <nav className="nav">
      <div className="brand">OAuth2 Integration</div>

      <div className="nav-links">
        {/* Hover container */}
        <div className="nav-item">
          <a href="/" className="btn link">Home</a>
        </div>

        {!loading && me && (
          <>
            <div className="nav-item">
              <a href="/profile" className="btn link">Profile</a>
            </div>

            <div className="nav-item">
              <button className="logout" onClick={() => logout()}>
                Logout
              </button>
            </div>
          </>
        )}
      </div>
    </nav>
  );
}
