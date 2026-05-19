import Link from "next/link";
import { notFound } from "next/navigation";
import { CreateTimelineEventForm } from "../../../components/CreateTimelineEventForm";
import { DownloadPostmortemButton } from "../../../components/DownloadPostmortemButton";
import { GeneratePostmortemButton } from "../../../components/GeneratePostmortemButton";
import { GenerateRecommendationButton } from "../../../components/GenerateRecommendationButton";
import { IncidentStatusWorkflow } from "../../../components/IncidentStatusWorkflow";
import { SessionMenu } from "../../../components/SessionMenu";
import { VoiceRoomPanel } from "../../../components/VoiceRoomPanel";
import { authHeaders, canWriteIncidents, requireAuthToken, requireCurrentUser } from "../../../lib/auth";

type IncidentStatus = "OPEN" | "MITIGATING" | "RESOLVED";

type Incident = {
  id: string;
  title: string;
  severity: string;
  status: IncidentStatus;
  startedAt: string;
};

type TimelineEvent = {
  id: string;
  incidentId: string;
  eventType: string;
  summary: string;
  source: string;
  createdAt: string;
};

type TranscriptSegment = {
  id: string;
  incidentId: string;
  speakerLabel: string;
  text: string;
  confidence: number;
  createdAt: string;
};

type AiRecommendation = {
  id: string;
  incidentId: string;
  prompt: string;
  response: string;
  confidence: number;
  citations: string;
  status: string;
  createdAt: string;
};

type Postmortem = {
  id: string;
  incidentId: string;
  title: string;
  content: string;
  generatedFrom: string;
  createdAt: string;
};

type RunbookMatch = {
  id: string;
  slug: string;
  title: string;
  serviceName: string;
  tags: string;
  excerpt: string;
  score: number;
  citation: string;
};

type RunbookSearchResponse = {
  incidentId: string;
  query: string;
  matches: RunbookMatch[];
};

type EventAuditLog = {
  id: string;
  eventId: string;
  eventType: string;
  topic: string;
  messageKey: string;
  aggregateId: string;
  aggregateType: string;
  payload: string;
  status: string;
  kafkaPartition: number | null;
  kafkaOffset: number | null;
  errorMessage: string | null;
  occurredAt: string;
  createdAt: string;
  publishedAt: string | null;
};

type IncidentPageProps = {
  params: {
    incidentId: string;
  };
};

async function getIncident(incidentId: string): Promise<Incident | null> {
  const apiBaseUrl = process.env.INCIDENT_API_BASE_URL ?? "http://localhost:8081";
  const token = requireAuthToken();
  const response = await fetch(`${apiBaseUrl}/api/incidents/${incidentId}`, {
    cache: "no-store",
    headers: authHeaders(token)
  });

  if (response.status === 404) {
    return null;
  }

  if (!response.ok) {
    throw new Error(`Incident API returned ${response.status}`);
  }

  return response.json();
}

async function getTimeline(incidentId: string): Promise<TimelineEvent[]> {
  const apiBaseUrl = process.env.INCIDENT_API_BASE_URL ?? "http://localhost:8081";
  const token = requireAuthToken();
  const response = await fetch(`${apiBaseUrl}/api/incidents/${incidentId}/timeline`, {
    cache: "no-store",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    throw new Error(`Timeline API returned ${response.status}`);
  }

  return response.json();
}

async function getTranscripts(incidentId: string): Promise<TranscriptSegment[]> {
  const apiBaseUrl = process.env.INCIDENT_API_BASE_URL ?? "http://localhost:8081";
  const token = requireAuthToken();
  const response = await fetch(`${apiBaseUrl}/api/incidents/${incidentId}/transcripts`, {
    cache: "no-store",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    throw new Error(`Transcript API returned ${response.status}`);
  }

  return response.json();
}

async function getRecommendations(incidentId: string): Promise<AiRecommendation[]> {
  const apiBaseUrl = process.env.INCIDENT_API_BASE_URL ?? "http://localhost:8081";
  const token = requireAuthToken();
  const response = await fetch(`${apiBaseUrl}/api/incidents/${incidentId}/recommendations`, {
    cache: "no-store",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    throw new Error(`Recommendation API returned ${response.status}`);
  }

  return response.json();
}

async function getPostmortems(incidentId: string): Promise<Postmortem[]> {
  const apiBaseUrl = process.env.INCIDENT_API_BASE_URL ?? "http://localhost:8081";
  const token = requireAuthToken();
  const response = await fetch(`${apiBaseUrl}/api/incidents/${incidentId}/postmortems`, {
    cache: "no-store",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    throw new Error(`Postmortem API returned ${response.status}`);
  }

  return response.json();
}

async function getRunbooks(incidentId: string): Promise<RunbookSearchResponse> {
  const apiBaseUrl = process.env.INCIDENT_API_BASE_URL ?? "http://localhost:8081";
  const token = requireAuthToken();
  const response = await fetch(`${apiBaseUrl}/api/incidents/${incidentId}/runbooks`, {
    cache: "no-store",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    throw new Error(`Runbook API returned ${response.status}`);
  }

  return response.json();
}

async function getEventAuditLogs(incidentId: string): Promise<EventAuditLog[]> {
  const apiBaseUrl = process.env.INCIDENT_API_BASE_URL ?? "http://localhost:8081";
  const token = requireAuthToken();
  const response = await fetch(`${apiBaseUrl}/api/incidents/${incidentId}/events`, {
    cache: "no-store",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    throw new Error(`Event audit API returned ${response.status}`);
  }

  return response.json();
}

export default async function IncidentDetailPage({ params }: IncidentPageProps) {
  const currentUser = requireCurrentUser();
  const canWrite = canWriteIncidents(currentUser.role);
  const [incident, timeline, transcripts, recommendations, postmortems, runbooks, eventAuditLogs] = await Promise.all([
    getIncident(params.incidentId),
    getTimeline(params.incidentId),
    getTranscripts(params.incidentId),
    getRecommendations(params.incidentId),
    getPostmortems(params.incidentId),
    getRunbooks(params.incidentId),
    getEventAuditLogs(params.incidentId)
  ]);

  if (!incident) {
    notFound();
  }

  return (
    <main style={{ padding: "56px", maxWidth: "1120px", margin: "0 auto" }}>
      <Link href="/" style={{ color: "#93c5fd", textDecoration: "none", fontWeight: 700 }}>
        Back to command center
      </Link>

      <section
        style={{
          marginTop: "28px",
          padding: "32px",
          borderRadius: "28px",
          background: "rgba(15, 23, 42, 0.82)",
          border: "1px solid rgba(148, 163, 184, 0.24)"
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", gap: "20px", alignItems: "start" }}>
          <div>
            <p style={{ color: "#93c5fd", fontWeight: 700, letterSpacing: "0.12em", textTransform: "uppercase" }}>
              Incident Room
            </p>
            <h1 style={{ fontSize: "48px", lineHeight: 1.05, margin: "12px 0" }}>{incident.title}</h1>
            <p style={{ color: "#cbd5e1", margin: 0 }}>Incident ID: {incident.id}</p>
          </div>

          <div style={{ display: "flex", gap: "8px" }}>
            <span
              style={{
                borderRadius: "999px",
                padding: "8px 12px",
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
                padding: "8px 12px",
                background: "rgba(34, 197, 94, 0.16)",
                color: "#bbf7d0",
                fontWeight: 700
              }}
            >
              {incident.status}
            </span>
          </div>
        </div>

        <p style={{ color: "#cbd5e1", marginTop: "24px" }}>
          Started {new Date(incident.startedAt).toLocaleString()}
        </p>
        <SessionMenu user={currentUser} />
        {canWrite ? (
          <IncidentStatusWorkflow incidentId={incident.id} currentStatus={incident.status} />
        ) : (
          <ReadOnlyNotice message="Viewer mode: changing incident status requires ADMIN or RESPONDER access." />
        )}
      </section>

      <section style={{ display: "grid", gap: "16px", marginTop: "24px" }}>
        <article
          style={{
            padding: "24px",
            borderRadius: "22px",
            background: "rgba(15, 23, 42, 0.72)",
            border: "1px solid rgba(148, 163, 184, 0.24)"
          }}
        >
          <h2 style={{ margin: "0 0 12px", fontSize: "24px" }}>Timeline</h2>
          <p style={{ color: "#cbd5e1", lineHeight: 1.6, margin: 0 }}>
            Capture decisions, mitigations, status updates, and action items as the incident unfolds.
          </p>

          {canWrite ? (
            <CreateTimelineEventForm incidentId={incident.id} />
          ) : (
            <ReadOnlyNotice message="Viewer mode: timeline notes require ADMIN or RESPONDER access." />
          )}

          {timeline.length === 0 ? (
            <p style={{ color: "#cbd5e1", marginTop: "20px" }}>No timeline events yet.</p>
          ) : (
            <div style={{ display: "grid", gap: "12px", marginTop: "20px" }}>
              {timeline.map((event) => (
                <div
                  key={event.id}
                  style={{
                    padding: "16px",
                    borderRadius: "16px",
                    background: "rgba(2, 6, 23, 0.46)",
                    border: "1px solid rgba(148, 163, 184, 0.16)"
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", gap: "12px" }}>
                    <span style={{ color: "#bfdbfe", fontWeight: 700 }}>{event.eventType.replace("_", " ")}</span>
                    <span style={{ color: "#94a3b8" }}>{new Date(event.createdAt).toLocaleString()}</span>
                  </div>
                  <p style={{ color: "#f8fafc", lineHeight: 1.6, margin: "10px 0" }}>{event.summary}</p>
                  <p style={{ color: "#94a3b8", margin: 0 }}>Source: {event.source}</p>
                </div>
              ))}
            </div>
          )}
        </article>

        {canWrite ? (
          <VoiceRoomPanel incidentId={incident.id} />
        ) : (
          <ReadOnlyPanel
            title="Voice Room"
            message="Viewer mode: microphone streaming and transcript capture require ADMIN or RESPONDER access."
          />
        )}

        <article
          style={{
            padding: "24px",
            borderRadius: "22px",
            background: "rgba(15, 23, 42, 0.72)",
            border: "1px solid rgba(148, 163, 184, 0.24)"
          }}
        >
          <h2 style={{ margin: "0 0 12px", fontSize: "24px" }}>Runbook Retrieval</h2>
          <p style={{ color: "#cbd5e1", lineHeight: 1.6, margin: 0 }}>
            Relevant operational docs retrieved from incident title, timeline, transcripts, and recommendations.
          </p>

          {runbooks.matches.length === 0 ? (
            <p style={{ color: "#cbd5e1", marginTop: "20px" }}>
              No runbook matches yet. Add timeline or transcript context with service/error details.
            </p>
          ) : (
            <div style={{ display: "grid", gap: "12px", marginTop: "20px" }}>
              {runbooks.matches.map((runbook) => (
                <div
                  key={runbook.id}
                  style={{
                    padding: "16px",
                    borderRadius: "16px",
                    background: "rgba(2, 6, 23, 0.46)",
                    border: "1px solid rgba(96, 165, 250, 0.24)"
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", gap: "12px", flexWrap: "wrap" }}>
                    <span style={{ color: "#bfdbfe", fontWeight: 700 }}>{runbook.title}</span>
                    <span style={{ color: "#93c5fd", fontWeight: 700 }}>{runbook.citation}</span>
                  </div>
                  <p style={{ color: "#94a3b8", margin: "8px 0 0" }}>
                    Service: {runbook.serviceName} | Score: {runbook.score} | Tags: {runbook.tags}
                  </p>
                  <p style={{ color: "#f8fafc", lineHeight: 1.6, margin: "10px 0 0" }}>{runbook.excerpt}</p>
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
          <h2 style={{ margin: "0 0 12px", fontSize: "24px" }}>AI Recommendations</h2>
          <p style={{ color: "#cbd5e1", lineHeight: 1.6, margin: 0 }}>
            Provider-neutral recommendation generation using the current incident timeline and transcript context.
          </p>

          {canWrite ? (
            <GenerateRecommendationButton incidentId={incident.id} />
          ) : (
            <ReadOnlyNotice message="Viewer mode: generating new AI recommendations requires ADMIN or RESPONDER access." />
          )}

          {recommendations.length === 0 ? (
            <p style={{ color: "#cbd5e1", marginTop: "20px" }}>
              No recommendations yet. Generate one after adding timeline or transcript context.
            </p>
          ) : (
            <div style={{ display: "grid", gap: "12px", marginTop: "20px" }}>
              {recommendations.map((recommendation) => (
                <div
                  key={recommendation.id}
                  style={{
                    padding: "16px",
                    borderRadius: "16px",
                    background: "rgba(2, 6, 23, 0.46)",
                    border: "1px solid rgba(167, 139, 250, 0.24)"
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", gap: "12px" }}>
                    <span style={{ color: "#ddd6fe", fontWeight: 700 }}>{recommendation.status}</span>
                    <span style={{ color: "#94a3b8" }}>{new Date(recommendation.createdAt).toLocaleString()}</span>
                  </div>
                  <p style={{ color: "#f8fafc", lineHeight: 1.6, whiteSpace: "pre-line" }}>
                    {recommendation.response}
                  </p>
                  <p style={{ color: "#94a3b8", margin: "8px 0 0" }}>
                    Confidence: {Math.round(Number(recommendation.confidence) * 100)}%
                  </p>
                  <p style={{ color: "#94a3b8", margin: "8px 0 0" }}>Citations: {recommendation.citations}</p>
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
          <h2 style={{ margin: "0 0 12px", fontSize: "24px" }}>Postmortem</h2>
          <p style={{ color: "#cbd5e1", lineHeight: 1.6, margin: 0 }}>
            Generate a post-incident report from timeline events, transcripts, AI recommendations, and status changes.
          </p>

          {canWrite ? (
            <GeneratePostmortemButton incidentId={incident.id} />
          ) : (
            <ReadOnlyNotice message="Viewer mode: generating a new postmortem requires ADMIN or RESPONDER access." />
          )}

          {postmortems.length === 0 ? (
            <p style={{ color: "#cbd5e1", marginTop: "20px" }}>
              No postmortems yet. Resolve the incident or add more context, then generate one.
            </p>
          ) : (
            <div style={{ display: "grid", gap: "12px", marginTop: "20px" }}>
              {postmortems.map((postmortem) => (
                <div
                  key={postmortem.id}
                  style={{
                    padding: "16px",
                    borderRadius: "16px",
                    background: "rgba(2, 6, 23, 0.46)",
                    border: "1px solid rgba(103, 232, 249, 0.24)"
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      gap: "12px",
                      flexWrap: "wrap",
                      alignItems: "center"
                    }}
                  >
                    <span style={{ color: "#a5f3fc", fontWeight: 700 }}>{postmortem.title}</span>
                    <div style={{ display: "flex", gap: "10px", alignItems: "center", flexWrap: "wrap" }}>
                      <span style={{ color: "#94a3b8" }}>{new Date(postmortem.createdAt).toLocaleString()}</span>
                      <DownloadPostmortemButton
                        title={postmortem.title}
                        content={postmortem.content}
                        createdAt={postmortem.createdAt}
                      />
                    </div>
                  </div>
                  <p style={{ color: "#94a3b8", margin: "8px 0 0" }}>Generated from: {postmortem.generatedFrom}</p>
                  <pre
                    style={{
                      color: "#f8fafc",
                      lineHeight: 1.6,
                      whiteSpace: "pre-wrap",
                      fontFamily: "inherit",
                      margin: "14px 0 0"
                    }}
                  >
                    {postmortem.content}
                  </pre>
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
          <h2 style={{ margin: "0 0 12px", fontSize: "24px" }}>Domain Event Audit Log</h2>
          <p style={{ color: "#cbd5e1", lineHeight: 1.6, margin: 0 }}>
            Persistent audit trail for Kafka domain events published by this incident.
          </p>

          {eventAuditLogs.length === 0 ? (
            <p style={{ color: "#cbd5e1", marginTop: "20px" }}>
              No domain events captured yet. Create a timeline note, stream audio, or generate an AI recommendation.
            </p>
          ) : (
            <div style={{ display: "grid", gap: "12px", marginTop: "20px" }}>
              {eventAuditLogs.slice(0, 12).map((event) => (
                <div
                  key={event.id}
                  style={{
                    padding: "16px",
                    borderRadius: "16px",
                    background: "rgba(2, 6, 23, 0.46)",
                    border: "1px solid rgba(148, 163, 184, 0.16)"
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", gap: "12px", flexWrap: "wrap" }}>
                    <span style={{ color: "#bfdbfe", fontWeight: 700 }}>{event.eventType}</span>
                    <span style={{ color: event.status === "PUBLISHED" ? "#bbf7d0" : "#fecaca", fontWeight: 700 }}>
                      {event.status}
                    </span>
                  </div>
                  <p style={{ color: "#cbd5e1", margin: "10px 0 0" }}>Topic: {event.topic}</p>
                  <p style={{ color: "#94a3b8", margin: "6px 0 0" }}>
                    Partition: {event.kafkaPartition ?? "pending"} | Offset: {event.kafkaOffset ?? "pending"}
                  </p>
                  {event.errorMessage ? (
                    <p style={{ color: "#fecaca", margin: "6px 0 0" }}>Error: {event.errorMessage}</p>
                  ) : null}
                  <p style={{ color: "#94a3b8", margin: "6px 0 0" }}>
                    Created {new Date(event.createdAt).toLocaleString()}
                  </p>
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
          <h2 style={{ margin: "0 0 12px", fontSize: "24px" }}>Transcript</h2>
          <p style={{ color: "#cbd5e1", lineHeight: 1.6, margin: 0 }}>
            Real speech-to-text transcript segments captured from the voice room.
          </p>

          {transcripts.length === 0 ? (
            <p style={{ color: "#cbd5e1", marginTop: "20px" }}>
              No transcript segments yet. Start the microphone to generate voice activity segments.
            </p>
          ) : (
            <div style={{ display: "grid", gap: "12px", marginTop: "20px" }}>
              {transcripts.map((segment) => (
                <div
                  key={segment.id}
                  style={{
                    padding: "16px",
                    borderRadius: "16px",
                    background: "rgba(2, 6, 23, 0.46)",
                    border: "1px solid rgba(148, 163, 184, 0.16)"
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", gap: "12px" }}>
                    <span style={{ color: "#bfdbfe", fontWeight: 700 }}>{segment.speakerLabel}</span>
                    <span style={{ color: "#94a3b8" }}>{new Date(segment.createdAt).toLocaleString()}</span>
                  </div>
                  <p style={{ color: "#f8fafc", lineHeight: 1.6, margin: "10px 0" }}>{segment.text}</p>
                  <p style={{ color: "#94a3b8", margin: 0 }}>
                    Confidence: {Math.round(Number(segment.confidence) * 100)}%
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

function ReadOnlyNotice({ message }: { message: string }) {
  return (
    <div
      style={{
        marginTop: "16px",
        padding: "16px",
        borderRadius: "16px",
        background: "rgba(15, 23, 42, 0.72)",
        border: "1px solid rgba(148, 163, 184, 0.24)",
        color: "#cbd5e1"
      }}
    >
      {message}
    </div>
  );
}

function ReadOnlyPanel({ title, message }: { title: string; message: string }) {
  return (
    <article
      style={{
        padding: "24px",
        borderRadius: "22px",
        background: "rgba(15, 23, 42, 0.72)",
        border: "1px solid rgba(148, 163, 184, 0.24)"
      }}
    >
      <h2 style={{ margin: "0 0 12px", fontSize: "24px" }}>{title}</h2>
      <p style={{ color: "#cbd5e1", lineHeight: 1.6, margin: 0 }}>{message}</p>
    </article>
  );
}
