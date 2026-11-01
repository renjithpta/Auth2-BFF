import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
function Flights({ api, user }) {
  const [rows, setRows] = useState([]),
    [busy, setBusy] = useState(true),
    [error, setError] = useState(""),
    [request, setRequest] = useState("");
  async function load() {
    setBusy(true);
    try {
      const x = await api("/flights");
      setRows(x.body.data);
      setRequest(x.requestId);
      setError("");
    } catch (e) {
      setError(`${e.status || ""} ${e.message} ${e.requestId || ""}`);
    } finally {
      setBusy(false);
    }
  }
  useEffect(() => {
    load();
  }, []);
  async function board(id) {
    try {
      const current = await api("/flights/" + id);
      await api("/flights/" + id, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          "If-Match": current.etag,
        },
        body: JSON.stringify({ status: "BOARDING" }),
      });
      await load();
    } catch (e) {
      setError(`${e.status || ""} ${e.message} ${e.requestId || ""}`);
    }
  }
  return (
    <>
      <div className="stats">
        <div className="stat">
          <small>AUTHORIZED FLIGHTS</small>
          <strong>{rows.length}</strong>
        </div>
        <div className="stat">
          <small>AIRPORT</small>
          <strong>BRU</strong>
        </div>
        <div className="stat">
          <small>ACCESS</small>
          <strong style={{ fontSize: 19 }}>Scoped by terminal</strong>
        </div>
      </div>
      <section className="panel">
        <div className="panel-head">
          <div>
            <h2>Operational flight board</h2>
            <span className="muted">
              Flight microfrontend · Independent release 1.0.0
            </span>
          </div>
          <button disabled={busy} onClick={load}>
            Refresh
          </button>
        </div>
        {error && (
          <div role="alert" className="error">
            {error}
          </div>
        )}
        {busy ? (
          <p role="status">Loading flights…</p>
        ) : rows.length === 0 ? (
          <p>No flights in your authorized scope.</p>
        ) : (
          <table>
            <thead>
              <tr>
                {[
                  "FLIGHT",
                  "TERMINAL",
                  "AIRLINE",
                  "STATUS",
                  "VERSION",
                  "ACTION",
                ].map((x) => (
                  <th key={x}>{x}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((f) => (
                <tr key={f.flightId}>
                  <td>
                    <strong>{f.flightNumber}</strong>
                    <div className="muted">{f.flightId}</div>
                  </td>
                  <td>{f.terminalId}</td>
                  <td>{f.airlineCode}</td>
                  <td>
                    <span className="badge">{f.status}</span>
                  </td>
                  <td>{f.version}</td>
                  <td>
                    {user.roles.includes("ROLE_FLIGHT_UPDATE") && (
                      <button
                        onClick={() => board(f.flightId)}
                        disabled={f.status === "DEPARTED"}
                      >
                        Set boarding
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <p className="muted">Request reference: {request || "—"}</p>
      </section>
    </>
  );
}
export function mount(element, context) {
  const root = createRoot(element);
  root.render(<Flights {...context} />);
  return () => root.unmount();
}
