import { NextResponse } from "next/server";
import { authHeaders, getAuthToken } from "../../../../../lib/auth";

const incidentApiBaseUrl = process.env.INCIDENT_API_BASE_URL ?? "http://localhost:8081";

type RouteContext = {
  params: {
    incidentId: string;
  };
};

export async function PUT(request: Request, { params }: RouteContext) {
  const token = getAuthToken();
  if (!token) {
    return NextResponse.json({ message: "Unauthorized" }, { status: 401 });
  }

  const payload = await request.json();

  const response = await fetch(`${incidentApiBaseUrl}/api/incidents/${params.incidentId}/status`, {
    method: "PUT",
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
