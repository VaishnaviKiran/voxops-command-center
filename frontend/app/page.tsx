import { CreateIncidentForm } from "../components/CreateIncidentForm";
import { DeleteIncidentButton } from "../components/DeleteIncidentButton";
import { SessionMenu } from "../components/SessionMenu";
import Link from "next/link";
import { authHeaders, canWriteIncidents, requireAuthToken, requireCurrentUser } from "../lib/auth";

const capabilities = [
  "Realtime incident voice room",
  "Live transcript and timeline",
  "RAG over runbooks and prior incidents",
  "LLM recommendations with citations",
  "Postmortem generation"
];

type Incident = {
  id: string;
  title: string;
  severity: string;
  status: string;
  startedAt: string;
};

async function getIncidents(): Promise<Incident[]> {
  const apiBaseUrl = process.env.INCIDENT_API_BASE_URL ?? "http://localhost:8081";
  const token = requireAuthToken();

  try {
    const response = await fetch(`${apiBaseUrl}/api/incidents`, {
      cache: "no-store",
      headers: authHeaders(token)
    });

    if (!response.ok) {
      throw new Error(`Incident API returned ${response.status}`);
    }

    return response.json();
  } catch (error) {
    console.error("Failed to load incidents", error);
    return [];
  }
}

export default async function HomePage() {
  const currentUser = requireCurrentUser();
  const canWrite = canWriteIncidents(currentUser.role);
  const incidents = await getIncidents();

  return (
    <main style={{ padding: "64px", maxWidth: "1120px", margin: "0 auto" }}>
      <section>
        <p style={{ color: "#93c5fd", fontWeight: 700, letterSpacing: "0.12em", textTransform: "uppercase" }}>
          VoxOps Command Center
        </p>
        <h1 style={{ fontSize: "64px", lineHeight: 1, margin: "16px 0", maxWidth: "900px" }}>
          Voice AI for production incident command.
        </h1>
        <p style={{ color: "#cbd5e1", fontSize: "20px", lineHeight: 1.6, maxWidth: "760px" }}>
          A Java 21 and Spring Boot platform that listens to live incident calls, understands decisions,
          retrieves operational knowledge, recommends next actions, and drafts postmortems.
        </p>
        <SessionMenu user={currentUser} />
        <Link
          href="/dashboard"
          style={{
            display: "inline-block",
            marginTop: "20px",
            borderRadius: "14px",
            padding: "13px 18px",
            background: "#0891b2",
            color: "#ffffff",
            fontWeight: 700,
            textDecoration: "none"
          }}
        >
          Open operations dashboard
        </Link>
      </section>

      <section
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
          gap: "16px",
          marginTop: "48px"
        }}
      >
        {capabilities.map((capability) => (
          <div
            key={capability}
            style={{
              border: "1px solid rgba(148, 163, 184, 0.24)",
              borderRadius: "20px",
              padding: "24px",
              background: "rgba(15, 23, 42, 0.72)"
            }}
          >
            <h2 style={{ fontSize: "18px", margin: 0 }}>{capability}</h2>
          </div>
        ))}
      </section>

      <section
        style={{
          marginTop: "48px",
          padding: "28px",
          borderRadius: "24px",
          background: "rgba(37, 99, 235, 0.16)",
          border: "1px solid rgba(96, 165, 250, 0.32)"
        }}
      >
        <h2 style={{ marginTop: 0 }}>First milestone</h2>
        <p style={{ color: "#dbeafe", lineHeight: 1.6 }}>
          The frontend now reads live incident data from the Java Spring Boot incident service.
        </p>
      </section>

      <section style={{ marginTop: "48px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", gap: "16px", alignItems: "end" }}>
          <div>
            <p style={{ color: "#93c5fd", fontWeight: 700, letterSpacing: "0.12em", textTransform: "uppercase" }}>
              Live Incidents
            </p>
            <h2 style={{ fontSize: "36px", margin: "8px 0" }}>Incident command feed</h2>
          </div>
          <span style={{ color: "#cbd5e1" }}>{incidents.length} active</span>
        </div>

        {canWrite ? (
          <CreateIncidentForm />
        ) : (
          <div
            style={{
              marginTop: "24px",
              padding: "20px",
              border: "1px solid rgba(148, 163, 184, 0.24)",
              borderRadius: "20px",
              background: "rgba(15, 23, 42, 0.72)",
              color: "#cbd5e1"
            }}
          >
            Viewer mode: you can read incidents, timelines, transcripts, recommendations, and event audits. Creating
            incidents requires ADMIN or RESPONDER access.
          </div>
        )}

        {incidents.length === 0 ? (
          <div
            style={{
              marginTop: "20px",
              padding: "24px",
              border: "1px solid rgba(248, 113, 113, 0.32)",
              borderRadius: "20px",
              background: "rgba(127, 29, 29, 0.22)",
              color: "#fecaca"
            }}
          >
            No incidents loaded. Make sure the incident service is running on port 8081.
          </div>
        ) : (
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
              gap: "16px",
              marginTop: "20px"
            }}
          >
            {incidents.map((incident) => (
              <div
                key={incident.id}
                style={{
                  border: "1px solid rgba(148, 163, 184, 0.24)",
                  borderRadius: "20px",
                  padding: "24px",
                  background: "rgba(15, 23, 42, 0.72)"
                }}
              >
                <div style={{ display: "flex", gap: "8px", marginBottom: "16px" }}>
                  <span
                    style={{
                      borderRadius: "999px",
                      padding: "6px 10px",
                      background: "rgba(239, 68, 68, 0.18)",
                      color: "#fecaca",
                      fontWeight: 700
                    }}
                  >
                    {incident.severity}
                  </span>
                  <span
                    style={{
                      borderRadius: "999px",
                      padding: "6px 10px",
                      background: "rgba(34, 197, 94, 0.16)",
                      color: "#bbf7d0",
                      fontWeight: 700
                    }}
                  >
                    {incident.status}
                  </span>
                </div>
                <h3 style={{ fontSize: "22px", margin: "0 0 12px", color: "#f8fafc" }}>{incident.title}</h3>
                <p style={{ color: "#cbd5e1", margin: "0 0 16px" }}>
                  Started {new Date(incident.startedAt).toLocaleString()}
                </p>
                <div style={{ display: "flex", gap: "12px", alignItems: "center", flexWrap: "wrap" }}>
                  <Link
                    href={`/incidents/${incident.id}`}
                    style={{ color: "#93c5fd", fontWeight: 700, textDecoration: "none" }}
                  >
                    Open incident room
                  </Link>
                  {canWrite ? (
                    <DeleteIncidentButton incidentId={incident.id} title={incident.title} />
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
