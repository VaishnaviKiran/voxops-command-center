"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

type GenerateRecommendationButtonProps = {
  incidentId: string;
};

export function GenerateRecommendationButton({ incidentId }: GenerateRecommendationButtonProps) {
  const router = useRouter();
  const [isGenerating, setIsGenerating] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  async function generateRecommendation() {
    setIsGenerating(true);
    setMessage(null);

    try {
      const response = await fetch(`/api/incidents/${incidentId}/recommendations/generate`, {
        method: "POST"
      });

      if (!response.ok) {
        throw new Error(`Generate recommendation failed with status ${response.status}`);
      }

      setMessage("Recommendation generated from current incident context.");
      router.refresh();
    } catch (error) {
      console.error(error);
      setMessage("Could not generate recommendation. Confirm incident-service is running.");
    } finally {
      setIsGenerating(false);
    }
  }

  return (
    <div style={{ marginTop: "16px" }}>
      <button
        disabled={isGenerating}
        onClick={generateRecommendation}
        style={{
          border: 0,
          borderRadius: "14px",
          padding: "13px 18px",
          background: isGenerating ? "#475569" : "#7c3aed",
          color: "#ffffff",
          cursor: isGenerating ? "not-allowed" : "pointer",
          fontWeight: 700
        }}
      >
        {isGenerating ? "Generating..." : "Generate recommendation"}
      </button>
      {message ? <p style={{ color: "#ddd6fe", marginBottom: 0 }}>{message}</p> : null}
    </div>
  );
}
