import { LoginForm } from "../../components/LoginForm";

export default function LoginPage() {
  return (
    <main style={{ padding: "64px", maxWidth: "760px", margin: "0 auto" }}>
      <p style={{ color: "#93c5fd", fontWeight: 700, letterSpacing: "0.12em", textTransform: "uppercase" }}>
        VoxOps Command Center
      </p>
      <h1 style={{ fontSize: "48px", lineHeight: 1.05, margin: "16px 0" }}>Sign in</h1>
      <p style={{ color: "#cbd5e1", lineHeight: 1.6 }}>
        Use a demo account to access protected incident command APIs.
      </p>

      <LoginForm />

      <section style={{ marginTop: "24px", color: "#cbd5e1", lineHeight: 1.7 }}>
        <h2 style={{ color: "#f8fafc" }}>Demo users</h2>
        <p>
          <strong>Admin:</strong> admin@voxops.dev / admin123
        </p>
        <p>
          <strong>Responder:</strong> responder@voxops.dev / responder123
        </p>
        <p>
          <strong>Viewer:</strong> viewer@voxops.dev / viewer123
        </p>
      </section>
    </main>
  );
}
