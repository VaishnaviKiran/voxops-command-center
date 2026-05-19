"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

const eventTypes = ["NOTE", "DECISION", "ACTION_ITEM", "MITIGATION", "STATUS_CHANGE"] as const;

type CreateTimelineEventFormProps = {
  incidentId: string;
};

export function CreateTimelineEventForm({ incidentId }: CreateTimelineEventFormProps) {
  const router = useRouter();
  const [eventType, setEventType] = useState<(typeof eventTypes)[number]>("NOTE");
  const [summary, setSummary] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setMessage(null);

    try {
      const response = await fetch(`/api/incidents/${incidentId}/timeline`, {
        method: "POST",
        headers: {
          "content-type": "application/json"
        },
        body: JSON.stringify({
          eventType,
          summary,
          source: "manual"
        })
      });

      if (!response.ok) {
        throw new Error(`Create timeline event failed with status ${response.status}`);
      }

      setEventType("NOTE");
      setSummary("");
      setMessage("Timeline event added.");
      router.refresh();
    } catch (error) {
      console.error(error);
      setMessage("Could not add timeline event. Confirm incident-service is running.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      style={{
        marginTop: "16px",
        padding: "20px",
        borderRadius: "18px",
        background: "rgba(2, 6, 23, 0.42)",
        border: "1px solid rgba(148, 163, 184, 0.18)"
      }}
    >
      <div style={{ display: "grid", gridTemplateColumns: "180px 1fr auto", gap: "12px", alignItems: "end" }}>
        <label style={{ display: "grid", gap: "8px", color: "#cbd5e1" }}>
          Event type
          <select
            value={eventType}
            onChange={(event) => setEventType(event.target.value as (typeof eventTypes)[number])}
            style={{
              border: "1px solid rgba(148, 163, 184, 0.32)",
              borderRadius: "14px",
              padding: "12px",
              background: "rgba(2, 6, 23, 0.72)",
              color: "#f8fafc"
            }}
          >
            {eventTypes.map((type) => (
              <option key={type} value={type}>
                {type.replace("_", " ")}
              </option>
            ))}
          </select>
        </label>

        <label style={{ display: "grid", gap: "8px", color: "#cbd5e1" }}>
          Summary
          <input
            required
            minLength={3}
            maxLength={2000}
            value={summary}
            onChange={(event) => setSummary(event.target.value)}
            placeholder="Example: Rolled back checkout deployment to previous version."
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
          {isSubmitting ? "Adding..." : "Add event"}
        </button>
      </div>

      {message ? <p style={{ color: "#bfdbfe", marginBottom: 0 }}>{message}</p> : null}
    </form>
  );
}
