# Deploy VoxOps (Vercel + Render)

This guide deploys:

| Piece | Host | Free tier |
|-------|------|-----------|
| Next.js frontend | **Vercel** | Yes |
| `incident-service` + Postgres | **Render** | Yes (with limits) |
| `voice-stream-service` | **Render** | Yes (second web service) |

**Not deployed on free tier:** Kafka, Grafana, Prometheus, Qdrant. Kafka is turned off in production (`VOXOPS_EVENTS_ENABLED=false`); audit log still records events as `DISABLED`.

---

## Before you start

1. GitHub repo is pushed: https://github.com/VaishnaviKiran/voxops-command-center  
2. Accounts: [render.com](https://render.com), [vercel.com](https://vercel.com) (sign in with GitHub).  
3. Expect **cold starts** on Render free web services (~30–60s) after idle.  
4. Run deploy steps in **browser dashboards**; use your PC terminal only when this doc says `git push`.

---

## Part A — Render PostgreSQL

1. Render Dashboard → **New +** → **PostgreSQL**  
2. Name: `voxops-db`, database `voxops`, user `voxops`, region near you, **Free** plan.  
3. Create. Open the database → **Info** tab.  
4. Copy **Internal Database URL** (starts with `postgresql://`).  
5. Build JDBC URL for Spring:

   If Internal URL is:
   `postgresql://voxops:PASSWORD@dpg-xxxxx-a/voxops`

   Use:
   ```text
   jdbc:postgresql://dpg-xxxxx-a/voxops
   ```
   (hostname only, no `postgresql://` prefix; username/password as separate env vars)

   Or use **External** host + port from the dashboard:
   ```text
   jdbc:postgresql://HOST:PORT/voxops
   ```

---

## Part B — Render `incident-service`

1. **New +** → **Web Service** → connect GitHub repo `voxops-command-center`.  
2. Settings:

   | Field | Value |
   |-------|--------|
   | Name | `voxops-incident-service` |
   | Region | Same as Postgres |
   | Root Directory | `backend` |
   | Runtime | **Docker** (recommended — avoids `mvn: command not found`) |
   | Dockerfile Path | `Dockerfile.incident-service` |
   | Build Command | *(leave empty — Docker builds the JAR)* |
   | Start Command | *(leave empty — defined in Dockerfile)* |

   If you use **Native** instead of Docker, Render must detect **Java**, not Node. If logs say `Using Node.js` and `mvn: command not found`, switch to Docker.
   | Plan | Free |

3. **Environment variables** (Environment tab):

   | Key | Value |
   |-----|--------|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST:PORT/voxops` (from Part A) |
   | `SPRING_DATASOURCE_USERNAME` | `voxops` |
   | `SPRING_DATASOURCE_PASSWORD` | (from Render Postgres) |
   | `VOXOPS_JWT_SECRET` | long random string (32+ chars) |
   | `VOXOPS_INTERNAL_SERVICE_TOKEN` | another long random string (save for voice service) |
   | `VOXOPS_AI_PROVIDER` | `MOCK` |
   | `VOXOPS_EVENTS_ENABLED` | `false` |

4. **Health Check Path:** `/actuator/health`  
5. Create Web Service. Wait until deploy is **Live**.  
6. Copy the public URL, e.g. `https://voxops-incident-service.onrender.com`  
7. Test: open `https://YOUR-INCIDENT-URL/actuator/health` → should show `"status":"UP"`.

---

## Part C — Render `voice-stream-service`

1. **New +** → **Web Service** → same repo.  
2. Settings:

   | Field | Value |
   |-------|--------|
   | Name | `voxops-voice-stream-service` |
   | Root Directory | `backend` |
   | Runtime | **Docker** |
   | Dockerfile Path | `Dockerfile.voice-stream-service` |
   | Build / Start Command | *(leave empty)* |
   | Plan | Free |

3. **Environment variables:**

   | Key | Value |
   |-----|--------|
   | `INCIDENT_SERVICE_BASE_URL` | `https://voxops-incident-service.onrender.com` (your Part B URL, **with** `https://`) |
   | `VOXOPS_INTERNAL_SERVICE_TOKEN` | **same** value as incident-service |

4. Health check: `/actuator/health`  
5. Deploy. Copy URL → `https://voxops-voice-stream-service.onrender.com`

---

## Part D — Vercel frontend

1. [vercel.com](https://vercel.com) → **Add New** → **Project** → import `voxops-command-center`.  
2. Settings:

   | Field | Value |
   |-------|--------|
   | Framework Preset | Next.js |
   | Root Directory | `frontend` |

3. **Environment variables** (Production):

   | Key | Value |
   |-----|--------|
   | `INCIDENT_API_BASE_URL` | `https://voxops-incident-service.onrender.com` |
   | `NEXT_PUBLIC_VOICE_WS_URL` | `wss://voxops-voice-stream-service.onrender.com/ws/voice` |

   Use your real Render hostnames. Voice URL must be **`wss://`** (not `ws://`).

4. Deploy. Open the Vercel URL (e.g. `https://voxops-command-center.vercel.app`).

---

## Part E — Smoke test

1. Open Vercel URL → **Login** → `admin@voxops.dev` / `admin123`  
2. Create an incident → timeline note → AI recommendation → status change  
3. Try microphone (needs HTTPS on Vercel; uses `wss://` voice service)  
4. If login fails: check Render logs for incident-service; confirm `INCIDENT_API_BASE_URL` on Vercel  

**Wake cold services:** first request after idle may take 30–60s — refresh once.

---

## Where to run commands

| Task | Where |
|------|--------|
| Push code changes | Cursor terminal: `cd C:\Users\vaish\voxops-command-center` → `git push` |
| Create Postgres / Web Services / env vars | **Render website** only |
| Connect repo & env vars | **Vercel website** only |
| Local demo | Your machine (`npm run dev`, Docker, etc.) |

After `git push`, Render and Vercel auto-redeploy if connected to GitHub.

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `mvn: command not found` / logs show Node.js | Change **Runtime** to **Docker**, Dockerfile `Dockerfile.incident-service`, root `backend` |
| Build fails: `common` not found | Docker build uses `-am`; or Native build command must include `-am` |
| Health check fails | Postgres URL/username wrong; check Render logs |
| Login works locally, not on Vercel | Set `INCIDENT_API_BASE_URL`; redeploy Vercel |
| Voice/mic fails | `NEXT_PUBLIC_VOICE_WS_URL` must be `wss://...`; voice service must be Live |
| Kafka errors in logs | Set `VOXOPS_EVENTS_ENABLED=false` and `SPRING_PROFILES_ACTIVE=prod` |

---

## Optional: Render Blueprint

Repo includes `render.yaml`. Advanced users can use **New Blueprint** on Render. First-time deploy is easier with Parts A–C above (manual).

After backend URLs exist, finish with **Part D (Vercel)**.
