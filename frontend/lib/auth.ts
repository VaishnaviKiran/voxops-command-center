import { cookies } from "next/headers";
import { redirect } from "next/navigation";

export const AUTH_COOKIE_NAME = "voxops_token";

export type AuthRole = "ADMIN" | "RESPONDER" | "VIEWER" | "SERVICE";

export type CurrentUser = {
  email: string;
  name: string;
  role: AuthRole;
};

export function getAuthToken() {
  return cookies().get(AUTH_COOKIE_NAME)?.value;
}

export function requireAuthToken() {
  const token = getAuthToken();
  if (!token) {
    redirect("/login");
  }
  return token;
}

export function authHeaders(token: string) {
  return {
    Authorization: `Bearer ${token}`
  };
}

export function getCurrentUser(): CurrentUser | null {
  const token = getAuthToken();
  if (!token) {
    return null;
  }

  return parseJwtUser(token);
}

export function requireCurrentUser() {
  const user = getCurrentUser();
  if (!user) {
    redirect("/login");
  }
  return user;
}

export function canWriteIncidents(role: AuthRole) {
  return role === "ADMIN" || role === "RESPONDER";
}

function parseJwtUser(token: string): CurrentUser | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) {
      return null;
    }

    const claims = JSON.parse(Buffer.from(payload, "base64url").toString("utf8")) as {
      sub?: string;
      name?: string;
      role?: AuthRole;
    };

    if (!claims.sub || !claims.name || !claims.role) {
      return null;
    }

    return {
      email: claims.sub,
      name: claims.name,
      role: claims.role
    };
  } catch {
    return null;
  }
}
