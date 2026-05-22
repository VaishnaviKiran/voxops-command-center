"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

type DeleteIncidentButtonProps = {
  incidentId: string;
  title: string;
};

export function DeleteIncidentButton({ incidentId, title }: DeleteIncidentButtonProps) {
  const router = useRouter();
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleDelete() {
    const confirmed = window.confirm(`Delete incident "${title}"? This cannot be undone.`);
    if (!confirmed) {
      return;
    }

    setIsDeleting(true);
    setError(null);

    try {
      const response = await fetch(`/api/incidents/${incidentId}`, {
        method: "DELETE"
      });

      if (!response.ok) {
        throw new Error(`Delete failed with status ${response.status}`);
      }

      router.refresh();
    } catch (deleteError) {
      console.error(deleteError);
      setError("Could not delete incident.");
    } finally {
      setIsDeleting(false);
    }
  }

  return (
    <div style={{ display: "grid", gap: "6px" }}>
      <button
        type="button"
        disabled={isDeleting}
        onClick={handleDelete}
        style={{
          border: "1px solid rgba(248, 113, 113, 0.45)",
          borderRadius: "12px",
          padding: "10px 14px",
          background: "rgba(127, 29, 29, 0.35)",
          color: "#fecaca",
          cursor: isDeleting ? "not-allowed" : "pointer",
          fontWeight: 700
        }}
      >
        {isDeleting ? "Deleting..." : "Delete"}
      </button>
      {error ? <span style={{ color: "#fecaca", fontSize: "13px" }}>{error}</span> : null}
    </div>
  );
}
