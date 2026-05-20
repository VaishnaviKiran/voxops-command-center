import { NextResponse } from "next/server";
import { AUTH_COOKIE_NAME } from "../../../../lib/auth";

const incidentApiBaseUrl = process.env.INCIDENT_API_BASE_URL ?? "http://localhost:8081";
const loginTimeoutMs = 90_000;

export async function POST(request: Request) {
  const payload = await request.json();

  if (process.env.NODE_ENV === "production" && incidentApiBaseUrl.includes("localhost")) {
    return NextResponse.json(
      {
        message:
          "INCIDENT_API_BASE_URL is not set on Vercel. Add https://voxops-incident-service.onrender.com and redeploy."
      },
      { status: 503 }
    );
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), loginTimeoutMs);

  let response: Response;
  try {
    response = await fetch(`${incidentApiBaseUrl}/api/auth/login`, {
      method: "POST",
      headers: {
        "content-type": "application/json"
      },
      body: JSON.stringify(payload),
      cache: "no-store",
      signal: controller.signal
    });
  } catch (error) {
    const message =
      error instanceof Error && error.name === "AbortError"
        ? "Incident service timed out. Open Render and wait for voxops-incident-service to wake up, then try again."
        : "Could not reach incident service. Check INCIDENT_API_BASE_URL on Vercel.";
    return NextResponse.json({ message }, { status: 503 });
  } finally {
    clearTimeout(timeout);
  }

  const body = await response.json();

  if (!response.ok) {
    return NextResponse.json(body, { status: response.status });
  }

  const nextResponse = NextResponse.json(body);
  const isProduction = process.env.NODE_ENV === "production";
  nextResponse.cookies.set(AUTH_COOKIE_NAME, body.accessToken, {
    httpOnly: true,
    sameSite: "lax",
    secure: isProduction,
    path: "/",
    maxAge: body.expiresInSeconds
  });

  return nextResponse;
}
