"use client";

import { FormEvent, useMemo, useState } from "react";
import { useRouter } from "next/navigation";

type IncidentStatus = "OPEN" | "MITIGATING" | "RESOLVED";

type IncidentStatusWorkflowProps = {
  incidentId: string;
  currentStatus: IncidentStatus;
};

const nextStatusesByCurrent: Record<IncidentStatus, IncidentStatus[]> = {
  OPEN: ["MITIGATING", "RESOLVED"],
  MITIGATING: ["RESOLVED", "OPEN"],
  RESOLVED: []
};

export function IncidentStatusWorkflow({ incidentId, currentStatus }: IncidentStatusWorkflowProps) {
  const router = useRouter();
  const nextStatuses = useMemo(() => nextStatusesByCurrent[currentStatus], [currentStatus]);
  const [selectedStatus, setSelectedStatus] = useState<IncidentStatus>(nextStatuses[0] ?? currentStatus);
  const [note, setNote] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  async function updateStatus(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (selectedStatus === currentStatus) {
      return;
    }

    setIsSubmitting(true);
    setMessage(null);

    try {
      const response = await fetch(`/api/incidents/${incidentId}/status`, {
        method: "PUT",
        headers: {
          "content-type": "application/json"
        },
        body: JSON.stringify({
          status: selectedStatus,
          note
        })
      });

      if (!response.ok) {
        throw new Error(`Update status failed with ${response.status}`);
      }

      setNote("");
      setMessage(`Incident moved to ${selectedStatus}.`);
      router.refresh();
    } catch (error) {
      console.error(error);
      setMessage("Could not update incident status. Check the transition and try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form
      onSubmit={updateStatus}
      style={{
        marginTop: "22px",
        padding: "18px",
        borderRadius: "18px",
        background: "rgba(2, 6, 23, 0.42)",
        border: "1px solid rgba(96, 165, 250, 0.24)"
      }}
    >
      <h2 style={{ margin: "0 0 12px", fontSize: "20px" }}>Incident Status Workflow</h2>
      <p style={{ color: "#cbd5e1", margin: "0 0 14px" }}>
        Current status: <strong style={{ color: "#bfdbfe" }}>{currentStatus}</strong>
      </p>

      {nextStatuses.length === 0 ? (
        <p style={{ color: "#cbd5e1", margin: 0 }}>This incident is resolved. No further transitions are available.</p>
      ) : (
        <>
          <div style={{ display: "grid", gridTemplateColumns: "180px 1fr auto", gap: "12px", alignItems: "end" }}>
            <label style={{ display: "grid", gap: "8px", color: "#cbd5e1" }}>
              Move to
              <select
                value={selectedStatus}
                onChange={(event) => setSelectedStatus(event.target.value as IncidentStatus)}
                style={{
                  border: "1px solid rgba(148, 163, 184, 0.32)",
                  borderRadius: "14px",
                  padding: "12px",
                  background: "rgba(2, 6, 23, 0.72)",
                  color: "#f8fafc"
                }}
              >
                {nextStatuses.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </label>

            <label style={{ display: "grid", gap: "8px", color: "#cbd5e1" }}>
              Transition note
              <input
                value={note}
                onChange={(event) => setNote(event.target.value)}
                maxLength={1000}
                placeholder="Example: Rollback is complete and error rate has recovered."
                style={{
                  border: "1px solid rgba(148, 163, 184, 0.32)",
                  borderRadius: "14px",
                  padding: "12px",
                  background: "rgba(2, 6, 23, 0.72)",
                  color: "#f8fafc"
                }}
              />
            </label>

            <button
              disabled={isSubmitting}
              type="submit"
              style={{
                border: 0,
                borderRadius: "14px",
                padding: "13px 18px",
                background: isSubmitting ? "#475569" : "#2563eb",
                color: "#ffffff",
                cursor: isSubmitting ? "not-allowed" : "pointer",
                fontWeight: 700
              }}
            >
              {isSubmitting ? "Updating..." : "Update status"}
            </button>
          </div>

          {message ? <p style={{ color: "#bfdbfe", marginBottom: 0 }}>{message}</p> : null}
        </>
      )}
    </form>
  );
}
