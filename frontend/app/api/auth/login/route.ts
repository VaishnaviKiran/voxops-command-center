import { NextResponse } from "next/server";
import { AUTH_COOKIE_NAME } from "../../../../lib/auth";

const incidentApiBaseUrl = process.env.INCIDENT_API_BASE_URL ?? "http://localhost:8081";

export async function POST(request: Request) {
  const payload = await request.json();

  const response = await fetch(`${incidentApiBaseUrl}/api/auth/login`, {
    method: "POST",
    headers: {
      "content-type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });

  const body = await response.json();

  if (!response.ok) {
    return NextResponse.json(body, { status: response.status });
  }

  const nextResponse = NextResponse.json(body);
  nextResponse.cookies.set(AUTH_COOKIE_NAME, body.accessToken, {
    httpOnly: true,
    sameSite: "lax",
    secure: false,
    path: "/",
    maxAge: body.expiresInSeconds
  });

  return nextResponse;
}
