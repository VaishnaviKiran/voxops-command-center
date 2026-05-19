"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

type GeneratePostmortemButtonProps = {
  incidentId: string;
};

export function GeneratePostmortemButton({ incidentId }: GeneratePostmortemButtonProps) {
  const router = useRouter();
  const [isGenerating, setIsGenerating] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  async function generatePostmortem() {
    setIsGenerating(true);
    setMessage(null);

    try {
      const response = await fetch(`/api/incidents/${incidentId}/postmortems/generate`, {
        method: "POST"
      });

      if (!response.ok) {
        throw new Error(`Generate postmortem failed with status ${response.status}`);
      }

      setMessage("Postmortem generated from current incident context.");
      router.refresh();
    } catch (error) {
      console.error(error);
      setMessage("Could not generate postmortem. Confirm incident-service is running.");
    } finally {
      setIsGenerating(false);
    }
  }

  return (
    <div style={{ marginTop: "16px" }}>
      <button
        disabled={isGenerating}
        onClick={generatePostmortem}
        style={{
          border: 0,
          borderRadius: "14px",
          padding: "13px 18px",
          background: isGenerating ? "#475569" : "#0891b2",
          color: "#ffffff",
          cursor: isGenerating ? "not-allowed" : "pointer",
          fontWeight: 700
        }}
      >
        {isGenerating ? "Generating..." : "Generate postmortem"}
      </button>
      {message ? <p style={{ color: "#a5f3fc", marginBottom: 0 }}>{message}</p> : null}
    </div>
  );
}
