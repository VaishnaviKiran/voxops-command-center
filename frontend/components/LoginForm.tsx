"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

export function LoginForm() {
  const router = useRouter();
  const [email, setEmail] = useState("admin@voxops.dev");
  const [password, setPassword] = useState("admin123");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: {
          "content-type": "application/json"
        },
        body: JSON.stringify({ email, password })
      });

      if (!response.ok) {
        throw new Error("Invalid credentials");
      }

      router.push("/");
      router.refresh();
    } catch (error) {
      console.error(error);
      setError("Login failed. Check the demo email and password.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      style={{
        display: "grid",
        gap: "16px",
        marginTop: "28px",
        padding: "24px",
        borderRadius: "24px",
        background: "rgba(15, 23, 42, 0.82)",
        border: "1px solid rgba(148, 163, 184, 0.24)"
      }}
    >
      <label style={{ display: "grid", gap: "8px", color: "#cbd5e1" }}>
        Email
        <input
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          style={{
            border: "1px solid rgba(148, 163, 184, 0.32)",
            borderRadius: "14px",
            padding: "12px 14px",
            background: "rgba(2, 6, 23, 0.72)",
            color: "#f8fafc"
          }}
        />
      </label>

      <label style={{ display: "grid", gap: "8px", color: "#cbd5e1" }}>
        Password
        <input
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          style={{
            border: "1px solid rgba(148, 163, 184, 0.32)",
            borderRadius: "14px",
            padding: "12px 14px",
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
        {isSubmitting ? "Signing in..." : "Sign in"}
      </button>

      {error ? <p style={{ color: "#fecaca", margin: 0 }}>{error}</p> : null}
    </form>
  );
}
