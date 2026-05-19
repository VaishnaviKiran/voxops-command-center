"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { CurrentUser } from "../lib/auth";

type SessionMenuProps = {
  user: CurrentUser;
};

export function SessionMenu({ user }: SessionMenuProps) {
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  async function logout() {
    setIsLoggingOut(true);

    try {
      await fetch("/api/auth/logout", {
        method: "POST"
      });
    } finally {
      router.push("/login");
      router.refresh();
    }
  }

  return (
    <div style={{ position: "relative", display: "inline-block", marginTop: "18px" }}>
      <button
        type="button"
        onClick={() => setIsOpen((current) => !current)}
        style={{
          display: "flex",
          alignItems: "center",
          gap: "10px",
          border: "1px solid rgba(147, 197, 253, 0.32)",
          borderRadius: "999px",
          padding: "10px 14px",
          background: "rgba(15, 23, 42, 0.82)",
          color: "#bfdbfe",
          cursor: "pointer",
          fontWeight: 700
        }}
      >
        <span>{user.name}</span>
        <span
          style={{
            borderRadius: "999px",
            padding: "4px 8px",
            background: "rgba(37, 99, 235, 0.32)",
            color: "#dbeafe",
            fontSize: "12px"
          }}
        >
          {user.role}
        </span>
      </button>

      {isOpen ? (
        <div
          style={{
            position: "absolute",
            zIndex: 20,
            top: "calc(100% + 10px)",
            left: 0,
            minWidth: "280px",
            padding: "16px",
            borderRadius: "18px",
            border: "1px solid rgba(148, 163, 184, 0.24)",
            background: "rgba(15, 23, 42, 0.98)",
            boxShadow: "0 18px 50px rgba(2, 6, 23, 0.45)"
          }}
        >
          <p style={{ color: "#f8fafc", fontWeight: 800, margin: "0 0 6px" }}>{user.name}</p>
          <p style={{ color: "#94a3b8", margin: "0 0 12px" }}>{user.email}</p>
          <p style={{ color: "#cbd5e1", margin: "0 0 16px" }}>
            Role: <strong style={{ color: "#bfdbfe" }}>{user.role}</strong>
          </p>

          <button
            type="button"
            disabled={isLoggingOut}
            onClick={logout}
            style={{
              width: "100%",
              border: "1px solid rgba(248, 113, 113, 0.42)",
              borderRadius: "14px",
              padding: "11px 14px",
              background: "rgba(127, 29, 29, 0.22)",
              color: "#fecaca",
              cursor: isLoggingOut ? "not-allowed" : "pointer",
              fontWeight: 700
            }}
          >
            {isLoggingOut ? "Logging out..." : "Log out"}
          </button>
        </div>
      ) : null}
    </div>
  );
}
