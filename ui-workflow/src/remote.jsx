import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
function Workflows({ api }) {
  const [data, setData] = useState(null),
    [error, setError] = useState("");
  useEffect(() => {
    api("/workflows")
      .then((x) => setData(x.body.data))
      .catch((e) => setError(`${e.status} ${e.message}`));
  }, []);
  return (
    <section className="panel">
      <small>WORKFLOW MICROFRONTEND · RELEASE 1.0.0</small>
      <h2>Operational tasks</h2>
      {error ? (
        <div role="alert" className="error">
          {error}
        </div>
      ) : data ? (
        <ul>
          {data.map((w) => (
            <li key={w.workflowId}>
              {w.workflowId} — {w.status}
            </li>
          ))}
        </ul>
      ) : (
        <p role="status">Loading workflows…</p>
      )}
    </section>
  );
}
export function mount(element, context) {
  const root = createRoot(element);
  root.render(<Workflows {...context} />);
  return () => root.unmount();
}
