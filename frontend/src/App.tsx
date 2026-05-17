import { useEffect, useState } from "react";
import "./App.css";
import "./lib/firebase";

type Status = "loading" | "ok" | "error";

function App() {
  const [status, setStatus] = useState<Status>("loading");
  const [detail, setDetail] = useState<string>("");

  useEffect(() => {
    // Relative URL → Firebase Hosting rewrite forwards /api/** to the api-gateway Cloud Run service.
    fetch("/api/healthz")
      .then(async (r) => {
        const body = await r.json();
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        setStatus(body.status === "ok" ? "ok" : "error");
        setDetail(JSON.stringify(body));
      })
      .catch((e: Error) => {
        setStatus("error");
        setDetail(e.message);
      });
  }, []);

  return (
    <main style={{ fontFamily: "system-ui, sans-serif", padding: "3rem", maxWidth: 640, margin: "0 auto" }}>
      <h1>DevTalks</h1>
      <p style={{ color: "#555" }}>
        Semantic search for cloud-native conference talks. Phase 0 healthcheck below.
      </p>
      <section
        style={{
          marginTop: "2rem",
          padding: "1rem 1.25rem",
          border: "1px solid #ddd",
          borderRadius: 8,
          background: status === "ok" ? "#ecfdf5" : status === "error" ? "#fef2f2" : "#f9fafb",
        }}
      >
        <strong>API status: </strong>
        <span data-testid="status">{status}</span>
        {detail && <pre style={{ marginTop: ".5rem", fontSize: 12, color: "#333" }}>{detail}</pre>}
      </section>
    </main>
  );
}

export default App;
