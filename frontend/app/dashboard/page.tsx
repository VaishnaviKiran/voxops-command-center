import Link from "next/link";
import { SessionMenu } from "../../components/SessionMenu";
import { authHeaders, requireAuthToken, requireCurrentUser } from "../../lib/auth";

type IncidentStatus = "OPEN" | "MITIGATING" | "RESOLVED";

type DashboardStatusCount = {
  status: IncidentStatus;
  count: number;
};

type DashboardMetrics = {
  totalIncidents: number;
  totalTimelineEvents: number;
  totalTranscripts: number;
  totalRecommendations: number;
  totalPostmortems: number;
  publishedKafkaEvents: number;
  failedKafkaEvents: number;
};

type DashboardRecentTimelineEvent = {
  incidentId: string;
  incidentTitle: string;
  eventType: string;
  summary: string;
  source: string;
  createdAt: string;
};

type DashboardRecentDomainEvent = {
  incidentId: string;
  eventType: string;
  topic: string;
  status: string;
  createdAt: string;
};

type DashboardSummary = {
  incidentsByStatus: DashboardStatusCount[];
  metrics: DashboardMetrics;
  recentTimelineEvents: DashboardRecentTimelineEvent[];
  recentDomainEvents: DashboardRecentDomainEvent[];
};

async function getDashboardSummary(): Promise<DashboardSummary> {
  const apiBaseUrl = process.env.INCIDENT_API_BASE_URL ?? "http://localhost:8081";
  const token = requireAuthToken();
  const response = await fetch(`${apiBaseUrl}/api/dashboard/summary`, {
    cache: "no-store",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    throw new Error(`Dashboard API returned ${response.status}`);
  }

  return response.json();
}

function statusColor(status: IncidentStatus) {
  if (status === "OPEN") {
    return { background: "rgba(239, 68, 68, 0.18)", color: "#fecaca" };
  }
  if (status === "MITIGATING") {
    return { background: "rgba(245, 158, 11, 0.18)", color: "#fde68a" };
  }
  return { background: "rgba(34, 197, 94, 0.16)", color: "#bbf7d0" };
}

export default async function DashboardPage() {
  const currentUser = requireCurrentUser();
  const summary = await getDashboardSummary();

  const metricCards = [
    { label: "Total incidents", value: summary.metrics.totalIncidents },
    { label: "Timeline events", value: summary.metrics.totalTimelineEvents },
    { label: "Transcript segments", value: summary.metrics.totalTranscripts },
    { label: "AI recommendations", value: summary.metrics.totalRecommendations },
    { label: "Postmortems", value: summary.metrics.totalPostmortems },
    { label: "Kafka events published", value: summary.metrics.publishedKafkaEvents },
    { label: "Kafka events failed", value: summary.metrics.failedKafkaEvents }
  ];

  return (
    <main style={{ padding: "56px", maxWidth: "1200px", margin: "0 auto" }}>
      <Link href="/" style={{ color: "#93c5fd", textDecoration: "none", fontWeight: 700 }}>
        Back to command center
      </Link>

      <section style={{ marginTop: "28px" }}>
        <p style={{ color: "#93c5fd", fontWeight: 700, letterSpacing: "0.12em", textTransform: "uppercase" }}>
          Operations Dashboard
        </p>
        <h1 style={{ fontSize: "48px", lineHeight: 1.05, margin: "12px 0" }}>Incident command summary</h1>
        <p style={{ color: "#cbd5e1", lineHeight: 1.6, maxWidth: "760px" }}>
          Live overview of incident status distribution, platform activity, and the most recent operational events.
        </p>
        <SessionMenu user={currentUser} />
      </section>

      <section
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
          gap: "16px",
          marginTop: "32px"
        }}
      >
        {summary.incidentsByStatus.map((item) => {
          const colors = statusColor(item.status);
          return (
            <div
              key={item.status}
              style={{
                padding: "24px",
                borderRadius: "20px",
                background: "rgba(15, 23, 42, 0.82)",
                border: "1px solid rgba(148, 163, 184, 0.24)"
              }}
            >
              <p style={{ color: "#94a3b8", margin: "0 0 8px" }}>{item.status}</p>
              <p style={{ fontSize: "40px", fontWeight: 800, margin: "0 0 12px" }}>{item.count}</p>
              <span
                style={{
                  borderRadius: "999px",
                  padding: "6px 10px",
                  fontWeight: 700,
                  ...colors
                }}
              >
                incidents
              </span>
            </div>
          );
        })}
      </section>

      <section
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
          gap: "16px",
          marginTop: "24px"
        }}
      >
        {metricCards.map((metric) => (
          <div
            key={metric.label}
            style={{
              padding: "20px",
              borderRadius: "18px",
              background: "rgba(2, 6, 23, 0.46)",
              border: "1px solid rgba(96, 165, 250, 0.24)"
            }}
          >
            <p style={{ color: "#94a3b8", margin: "0 0 8px" }}>{metric.label}</p>
            <p style={{ fontSize: "32px", fontWeight: 800, margin: 0, color: "#bfdbfe" }}>{metric.value}</p>
          </div>
        ))}
      </section>

      <section
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(340px, 1fr))",
          gap: "16px",
          marginTop: "24px"
        }}
      >
        <article
          style={{
            padding: "24px",
            borderRadius: "22px",
            background: "rgba(15, 23, 42, 0.72)",
            border: "1px solid rgba(148, 163, 184, 0.24)"
          }}
        >
          <h2 style={{ margin: "0 0 12px", fontSize: "24px" }}>Recent timeline events</h2>
          {summary.recentTimelineEvents.length === 0 ? (
            <p style={{ color: "#cbd5e1" }}>No timeline activity yet.</p>
          ) : (
            <div style={{ display: "grid", gap: "12px" }}>
              {summary.recentTimelineEvents.map((event) => (
                <div
                  key={`${event.incidentId}-${event.createdAt}-${event.summary}`}
                  style={{
                    padding: "14px",
                    borderRadius: "14px",
                    background: "rgba(2, 6, 23, 0.46)",
                    border: "1px solid rgba(148, 163, 184, 0.16)"
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", gap: "12px", flexWrap: "wrap" }}>
                    <Link
                      href={`/incidents/${event.incidentId}`}
                      style={{ color: "#93c5fd", fontWeight: 700, textDecoration: "none" }}
                    >
                      {event.incidentTitle}
                    </Link>
                    <span style={{ color: "#94a3b8" }}>{new Date(event.createdAt).toLocaleString()}</span>
                  </div>
                  <p style={{ color: "#bfdbfe", fontWeight: 700, margin: "8px 0 4px" }}>
                    {event.eventType.replace("_", " ")}
                  </p>
                  <p style={{ color: "#f8fafc", lineHeight: 1.5, margin: 0 }}>{event.summary}</p>
                </div>
              ))}
            </div>
          )}
        </article>

        <article
          style={{
            padding: "24px",
            borderRadius: "22px",
            background: "rgba(15, 23, 42, 0.72)",
            border: "1px solid rgba(148, 163, 184, 0.24)"
          }}
        >
          <h2 style={{ margin: "0 0 12px", fontSize: "24px" }}>Recent domain events</h2>
          {summary.recentDomainEvents.length === 0 ? (
            <p style={{ color: "#cbd5e1" }}>No domain events captured yet.</p>
          ) : (
            <div style={{ display: "grid", gap: "12px" }}>
              {summary.recentDomainEvents.map((event) => (
                <div
                  key={`${event.incidentId}-${event.createdAt}-${event.eventType}`}
                  style={{
                    padding: "14px",
                    borderRadius: "14px",
                    background: "rgba(2, 6, 23, 0.46)",
                    border: "1px solid rgba(148, 163, 184, 0.16)"
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", gap: "12px", flexWrap: "wrap" }}>
                    <span style={{ color: "#bfdbfe", fontWeight: 700 }}>{event.eventType}</span>
                    <span
                      style={{
                        color: event.status === "PUBLISHED" ? "#bbf7d0" : "#fecaca",
                        fontWeight: 700
                      }}
                    >
                      {event.status}
                    </span>
                  </div>
                  <p style={{ color: "#cbd5e1", margin: "8px 0 0" }}>Topic: {event.topic}</p>
                  <p style={{ color: "#94a3b8", margin: "6px 0 0" }}>
                    <Link href={`/incidents/${event.incidentId}`} style={{ color: "#93c5fd", textDecoration: "none" }}>
                      Open incident
                    </Link>
                    {" · "}
                    {new Date(event.createdAt).toLocaleString()}
                  </p>
                </div>
              ))}
            </div>
          )}
        </article>
      </section>
    </main>
  );
}
