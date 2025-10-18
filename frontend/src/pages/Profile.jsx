import { useEffect, useState } from "react";
import { getMe, updateProfile, logout } from "../lib/api";

export default function Profile() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [me, setMe] = useState(null);

  const [displayName, setDisplayName] = useState("");
  const [bio, setBio] = useState("");
  const [msg, setMsg] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        const data = await getMe();
        if (!data?.authenticated) {
          setMe(null);
        } else {
          setMe(data);
          setDisplayName(data.name || "");
          setBio(data.bio || "");
        }
      } catch {
        setMe(null);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const onSave = async (e) => {
    e.preventDefault();
    if (saving) return;
    setMsg(null);
    setSaving(true);
    try {
      const res = await updateProfile({ displayName, bio });
      setMe((m) => (m ? { ...m, name: res.displayName ?? displayName } : m));
      setBio(res.bio ?? bio);
      setMsg({ type: "ok", text: "Saved successfully." });
    } catch {
      setMsg({ type: "err", text: "Save failed. Please try again." });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="page">
        <div className="card">Loading…</div>
      </div>
    );
  }

  if (!me) {
    return (
      <div className="page">
        <div className="card">
          <div className="eyebrow">Profile</div>
          <h1 className="title">My Profile</h1>
          <p style={{ color: "var(--muted)", marginBottom: "12px" }}>
            You're not signed in.
          </p>
          <p style={{ color: "var(--muted)" }}>
            Go back to <a href="/">Home</a> and continue with Google or GitHub.
          </p>
        </div>
      </div>
    );
  }

  const avatar =
    me.picture ||
    `https://ui-avatars.com/api/?name=${encodeURIComponent(
      me.name || me.email || "User"
    )}&background=E5E7EB&color=111827`;

  return (
    <div className="page">
      <div className="card">
        <h1 className="title">My Profile</h1>

        <div className="profile-container">
          {/* Left: Avatar and Email */}
          <div className="profile-left">
            <img className="avatar" src={avatar} alt="avatar" />
            <div className="meta">
              <strong>Email</strong>
              {me.email}
            </div>
          </div>

          {/* Right: Editable Form */}
          <div className="profile-right">
            <form className="form" onSubmit={onSave}>
              <div className="form-grid">
                <div className="form-group">
                  <label htmlFor="displayName">Display Name</label>
                  <input
                    id="displayName"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    placeholder="Your display name"
                  />
                </div>

                <div className="form-group full-width">
                  <label htmlFor="bio">Bio</label>
                  <textarea
                    id="bio"
                    value={bio}
                    onChange={(e) => setBio(e.target.value)}
                    placeholder="Tell something about yourself..."
                  />
                </div>
              </div>

              <div className="form-actions">
                <button className="btn primary" type="submit" disabled={saving}>
                  {saving ? "Saving..." : "Save Changes"}
                </button>
              </div>
            </form>

            {msg && (
              <div className={`notice ${msg.type === "ok" ? "success" : "error"}`}>
                {msg.text}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}