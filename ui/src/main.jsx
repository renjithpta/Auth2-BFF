import React, { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import "./style.css";
async function api(path, options = {}) {
  const headers = { ...options.headers };
  if (options.method && options.method !== "GET") {
    const r = await fetch("/bff/api/v1/csrf");
    if (!r.ok) throw Error("CSRF initialization failed");
    const csrf = await r.json();
    headers[csrf.headerName] = csrf.token;
  }
  const r = await fetch("/bff/api/v1" + path, { ...options, headers });
  const text = await r.text();
  let body;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = { detail: "Unexpected server response" };
  }
  if (!r.ok)
    throw Object.assign(Error(body?.detail || "Request failed"), {
      status: r.status,
      requestId: r.headers.get("X-Request-ID"),
    });
  return {
    body,
    etag: r.headers.get("ETag"),
    requestId: r.headers.get("X-Request-ID"),
  };
}
function App() {
  const [user, setUser] = useState(null),
    [tab, setTab] = useState("flight"),
    [state, setState] = useState("Connecting"),
    [error, setError] = useState(""),
    [reload, setReload] = useState(0);
  const host = useRef(null);
  useEffect(() => {
    api("/session")
      .then((x) => {
        setUser(x.body.data);
        setState("Authenticated");
      })
      .catch((e) => {
        setState(e.status === 401 ? "Sign in required" : "Service unavailable");
        if (e.status !== 401) setError(e.message);
      });
  }, []);
  useEffect(() => {
    if (!user) return;
    let disposed = false,
      unmount;
    setError("");
    fetch("/mfe-manifest.json", { cache: "no-store" })
      .then((r) => {
        if (!r.ok) throw Error("Manifest unavailable");
        return r.json();
      })
      .then(async (manifest) => {
        const remote = manifest[tab];
        if (!remote?.url.startsWith("/mfes/") || remote.url.startsWith("//"))
          throw Error("Remote origin rejected");
        const module = await import(/* @vite-ignore */ remote.url);
        if (!disposed)
          unmount = module.mount(host.current, { api, user, refresh: reload });
      })
      .catch((e) => {
        if (!disposed) setError("Microfrontend could not load: " + e.message);
      });
    return () => {
      disposed = true;
      unmount?.();
    };
  }, [user, tab, reload]);
  useEffect(() => {
    if (!user) return;
    let stopped = false,
      socket,
      timer,
      attempt = 0;
    function connect() {
      socket = new WebSocket(
        (location.protocol === "https:" ? "wss:" : "ws:") +
          "//" +
          location.host +
          "/bff/ws/v1",
      );
      socket.onopen = () => {
        attempt = 0;
        setState("Live connection");
      };
      socket.onmessage = (e) => {
        try {
          if (JSON.parse(e.data).type === "REFRESH_HINT")
            setReload((x) => x + 1);
        } catch {
          setError("Invalid realtime message");
        }
      };
      socket.onclose = () => {
        setState("Reconnecting");
        if (!stopped)
          timer = setTimeout(
            () =>
              api("/session")
                .then(connect)
                .catch(() => setState("Session ended — sign in again")),
            Math.min(30000, 1000 * 2 ** attempt++) + Math.random() * 500,
          );
      };
    }
    connect();
    return () => {
      stopped = true;
      clearTimeout(timer);
      socket?.close();
    };
  }, [user]);
  async function logout() {
    try {
      const r = await fetch("/bff/api/v1/csrf");
      const t = await r.json();
      const f = document.createElement("form");
      f.method = "POST";
      f.action = "/logout";
      const i = document.createElement("input");
      i.type = "hidden";
      i.name = "_csrf";
      i.value = t.token;
      f.append(i);
      document.body.append(f);
      f.submit();
    } catch {
      setError("Logout could not be completed. Try again.");
    }
  }
  return (
    <div className="app">
      <aside>
        <div className="brand">
          AOBP<span>OPERATIONS PLATFORM</span>
        </div>
        <div className="airport">
          BRU <span>Brussels Airport</span>
        </div>
        <nav>
          {["flight", "workflow"].map((t) => (
            <button
              key={t}
              className={tab === t ? "active" : ""}
              onClick={() => setTab(t)}
            >
              {t === "flight" ? "Flight information" : "Workflow operations"}
            </button>
          ))}
        </nav>
        <div className="boundary">
          Secure browser session
          <br />
          Tokens stay in the BFF
        </div>
      </aside>
      <main>
        <header>
          <div>
            <small>OPERATIONS / {tab.toUpperCase()}</small>
            <h1>
              {tab === "flight" ? "Flight information" : "Workflow operations"}
            </h1>
          </div>
          <div className="identity">
            <span className="badge">{state}</span>
            {user ? (
              <>
                <strong>{user.name}</strong>
                <button onClick={logout}>Sign out</button>
              </>
            ) : (
              <a className="button" href="/oauth2/authorization/aobp">
                Sign in with Keycloak
              </a>
            )}
          </div>
        </header>
        {error && (
          <div role="alert" className="error">
            {error}
          </div>
        )}
        {user ? (
          <div ref={host} />
        ) : (
          <section className="welcome">
            <small>IDENTITY PROTECTED WORKSPACE</small>
            <h2>Your airport operations, in one place.</h2>
            <p>
              Sign in to view flights and tasks within your assigned operational
              scope.
            </p>
            <a className="button" href="/oauth2/authorization/aobp">
              Open secure workspace →
            </a>
          </section>
        )}
        <footer>
          Independent microfrontends · Session-based BFF · Domain authorization
        </footer>
      </main>
    </div>
  );
}
createRoot(document.getElementById("root")).render(<App />);
