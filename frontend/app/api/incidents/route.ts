import { NextResponse } from "next/server";
import { authHeaders, getAuthToken } from "../../../lib/auth";

const incidentApiBaseUrl = process.env.INCIDENT_API_BASE_URL ?? "http://localhost:8081";

export async function GET() {
  const token = getAuthToken();
  if (!token) {
    return NextResponse.json({ message: "Unauthorized" }, { status: 401 });
  }

  const response = await fetch(`${incidentApiBaseUrl}/api/incidents`, {
    cache: "no-store",
    headers: authHeaders(token)
  });

  const body = await response.text();

  return new NextResponse(body, {
    status: response.status,
    headers: {
      "content-type": response.headers.get("content-type") ?? "application/json"
    }
  });
}

export async function POST(request: Request) {
  const token = getAuthToken();
  if (!token) {
    return NextResponse.json({ message: "Unauthorized" }, { status: 401 });
  }

  const payload = await request.json();

  const response = await fetch(`${incidentApiBaseUrl}/api/incidents`, {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "content-type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });

  const body = await response.text();

  return new NextResponse(body, {
    status: response.status,
    headers: {
      "content-type": response.headers.get("content-type") ?? "application/json"
    }
  });
}
