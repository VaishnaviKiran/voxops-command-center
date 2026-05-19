"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

const severities = ["SEV1", "SEV2", "SEV3", "SEV4"] as const;

export function CreateIncidentForm() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [severity, setSeverity] = useState<(typeof severities)[number]>("SEV2");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setMessage(null);

    try {
      const response = await fetch("/api/incidents", {
        method: "POST",
        headers: {
          "content-type": "application/json"
        },
        body: JSON.stringify({
          title,
          severity
        })
      });

      if (!response.ok) {
        throw new Error(`Create incident failed with status ${response.status}`);
      }

      setTitle("");
      setSeverity("SEV2");
      setMessage("Incident created. The feed has been refreshed.");
      router.refresh();
    } catch (error) {
      console.error(error);
      setMessage("Could not create incident. Confirm incident-service is running on port 8081.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      style={{
        marginTop: "24px",
        padding: "24px",
        border: "1px solid rgba(96, 165, 250, 0.32)",
        borderRadius: "24px",
        background: "rgba(15, 23, 42, 0.82)"
      }}
    >
      <h3 style={{ margin: "0 0 16px", fontSize: "22px" }}>Create incident</h3>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "minmax(220px, 1fr) 160px auto",
          gap: "12px",
          alignItems: "end"
        }}
      >
        <label style={{ display: "grid", gap: "8px", color: "#cbd5e1" }}>
          Incident title
          <input
            required
            minLength={3}
            maxLength={255}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="Example: Search API latency spike"
            style={{
              width: "100%",
              border: "1px solid rgba(148, 163, 184, 0.32)",
              borderRadius: "14px",
              padding: "12px 14px",
              background: "rgba(2, 6, 23, 0.72)",
              color: "#f8fafc"
            }}
          />
        </label>

        <label style={{ display: "grid", gap: "8px", color: "#cbd5e1" }}>
          Severity
          <select
            value={severity}
            onChange={(event) => setSeverity(event.target.value as (typeof severities)[number])}
            style={{
              width: "100%",
              border: "1px solid rgba(148, 163, 184, 0.32)",
              borderRadius: "14px",
              padding: "12px 14px",
              background: "rgba(2, 6, 23, 0.72)",
              color: "#f8fafc"
            }}
          >
            {severities.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
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
          {isSubmitting ? "Creating..." : "Create"}
        </button>
      </div>

      {message ? <p style={{ color: "#bfdbfe", marginBottom: 0 }}>{message}</p> : null}
    </form>
  );
}
