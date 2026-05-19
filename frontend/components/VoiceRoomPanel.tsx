"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";

type VoiceRoomPanelProps = {
  incidentId: string;
};

type ConnectionState = "idle" | "connecting" | "streaming" | "stopped" | "error";
type SpeechRecognitionState = "unsupported" | "idle" | "listening" | "stopped" | "error";

type BrowserSpeechRecognition = {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  start: () => void;
  stop: () => void;
  onresult: ((event: BrowserSpeechRecognitionEvent) => void) | null;
  onerror: ((event: BrowserSpeechRecognitionErrorEvent) => void) | null;
  onend: (() => void) | null;
};

type BrowserSpeechRecognitionEvent = {
  resultIndex: number;
  results: {
    length: number;
    [index: number]: {
      isFinal: boolean;
      0: {
        transcript: string;
        confidence: number;
      };
    };
  };
};

type BrowserSpeechRecognitionErrorEvent = {
  error: string;
};

declare global {
  interface Window {
    SpeechRecognition?: new () => BrowserSpeechRecognition;
    webkitSpeechRecognition?: new () => BrowserSpeechRecognition;
  }
}

const voiceWebSocketUrl = process.env.NEXT_PUBLIC_VOICE_WS_URL ?? "ws://localhost:8082/ws/voice";

export function VoiceRoomPanel({ incidentId }: VoiceRoomPanelProps) {
  const router = useRouter();
  const socketRef = useRef<WebSocket | null>(null);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const recognitionRef = useRef<BrowserSpeechRecognition | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const intentionalStopRef = useRef(false);
  const [connectionState, setConnectionState] = useState<ConnectionState>("idle");
  const [speechRecognitionState, setSpeechRecognitionState] = useState<SpeechRecognitionState>("idle");
  const [chunksSent, setChunksSent] = useState(0);
  const [transcriptsCaptured, setTranscriptsCaptured] = useState(0);
  const [interimTranscript, setInterimTranscript] = useState("");
  const [lastAck, setLastAck] = useState("No audio sent yet.");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [speechMessage, setSpeechMessage] = useState("Speech recognition has not started.");

  async function startStreaming() {
    setConnectionState("connecting");
    setSpeechRecognitionState("idle");
    setErrorMessage(null);
    setInterimTranscript("");
    setLastAck("Opening microphone and WebSocket...");
    setSpeechMessage("Starting browser speech recognition...");
    intentionalStopRef.current = false;

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;
      startSpeechRecognition();

      const socket = new WebSocket(`${voiceWebSocketUrl}?incidentId=${incidentId}`);
      socket.binaryType = "arraybuffer";
      socketRef.current = socket;

      socket.onopen = () => {
        const mimeType = MediaRecorder.isTypeSupported("audio/webm;codecs=opus")
          ? "audio/webm;codecs=opus"
          : "audio/webm";
        const recorder = new MediaRecorder(stream, { mimeType });
        recorderRef.current = recorder;

        recorder.ondataavailable = async (event) => {
          if (event.data.size === 0 || socket.readyState !== WebSocket.OPEN) {
            return;
          }

          socket.send(await event.data.arrayBuffer());
          setChunksSent((count) => count + 1);
        };

        recorder.start(500);
        setConnectionState("streaming");
        setLastAck("Streaming audio chunks to voice-stream-service...");
      };

      socket.onmessage = (event) => {
        setLastAck(String(event.data));
      };

      socket.onerror = () => {
        setConnectionState("error");
        setErrorMessage("WebSocket error. Make sure voice-stream-service is running on port 8082.");
        cleanupAudioResources();
      };

      socket.onclose = (event) => {
        cleanupAudioResources();
        if (intentionalStopRef.current) {
          setConnectionState("stopped");
          setLastAck("Voice stream stopped.");
        } else {
          setConnectionState("error");
          setErrorMessage(`WebSocket closed unexpectedly. Code ${event.code}${event.reason ? `: ${event.reason}` : ""}`);
        }
      };
    } catch (error) {
      console.error(error);
      setConnectionState("error");
      setErrorMessage("Could not access microphone. Check browser permissions and try again.");
      stopStreaming();
    }
  }

  function startSpeechRecognition() {
    const SpeechRecognition = window.SpeechRecognition ?? window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      setSpeechRecognitionState("unsupported");
      setSpeechMessage("Speech recognition is not supported in this browser. Use Chrome for real transcript capture.");
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = "en-US";
    recognitionRef.current = recognition;

    recognition.onresult = (event) => {
      let interimText = "";

      for (let index = event.resultIndex; index < event.results.length; index += 1) {
        const result = event.results[index];
        const transcript = result[0].transcript.trim();

        if (!transcript) {
          continue;
        }

        if (result.isFinal) {
          setInterimTranscript("");
          void publishTranscript(transcript, result[0].confidence);
        } else {
          interimText += transcript;
        }
      }

      if (interimText) {
        setInterimTranscript(interimText);
      }
    };

    recognition.onerror = (event) => {
      setSpeechRecognitionState("error");
      setSpeechMessage(`Speech recognition error: ${event.error}`);
    };

    recognition.onend = () => {
      setSpeechRecognitionState((current) => (current === "error" || current === "unsupported" ? current : "stopped"));
      if (!intentionalStopRef.current && connectionState === "streaming") {
        setSpeechMessage("Speech recognition stopped. Click Stop, then Start microphone to resume transcripts.");
      }
    };

    try {
      recognition.start();
      setSpeechRecognitionState("listening");
      setSpeechMessage("Listening for real speech and saving final transcript phrases...");
    } catch (error) {
      console.error(error);
      setSpeechRecognitionState("error");
      setSpeechMessage("Speech recognition could not start. Stop and start the microphone again.");
    }
  }

  async function publishTranscript(text: string, confidence: number) {
    try {
      const response = await fetch(`/api/incidents/${incidentId}/transcripts`, {
        method: "POST",
        headers: {
          "content-type": "application/json"
        },
        body: JSON.stringify({
          speakerLabel: "Browser speech recognition",
          text,
          confidence: Number.isFinite(confidence) && confidence > 0 ? confidence : 0.85
        })
      });

      if (!response.ok) {
        throw new Error(`Transcript API returned ${response.status}`);
      }

      setTranscriptsCaptured((count) => count + 1);
      setSpeechMessage(`Saved transcript: "${text}"`);
      router.refresh();
    } catch (error) {
      console.error(error);
      setSpeechRecognitionState("error");
      setSpeechMessage("Could not save transcript. Make sure you are logged in and incident-service is running.");
    }
  }

  function stopStreaming() {
    intentionalStopRef.current = true;
    stopSpeechRecognition();
    cleanupAudioResources();
    socketRef.current?.close(1000, "user stopped");
    socketRef.current = null;

    setConnectionState((current) => (current === "error" ? "error" : "stopped"));
  }

  function stopSpeechRecognition() {
    recognitionRef.current?.stop();
    recognitionRef.current = null;
    setSpeechRecognitionState((current) => (current === "unsupported" ? "unsupported" : "stopped"));
    setInterimTranscript("");
  }

  function cleanupAudioResources() {
    if (recorderRef.current?.state !== "inactive") {
      recorderRef.current?.stop();
    }
    recorderRef.current = null;

    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
  }

  const isStreaming = connectionState === "streaming" || connectionState === "connecting";

  return (
    <article
      style={{
        padding: "24px",
        borderRadius: "22px",
        background: "rgba(15, 23, 42, 0.72)",
        border: "1px solid rgba(148, 163, 184, 0.24)"
      }}
    >
      <div style={{ display: "flex", justifyContent: "space-between", gap: "16px", alignItems: "start" }}>
        <div>
          <h2 style={{ margin: "0 0 12px", fontSize: "24px" }}>Voice Room</h2>
          <p style={{ color: "#cbd5e1", lineHeight: 1.6, margin: 0 }}>
            Stream microphone audio to the Java WebSocket service while Chrome speech recognition saves real transcript
            phrases into the incident timeline.
          </p>
        </div>
        <span
          style={{
            borderRadius: "999px",
            padding: "8px 12px",
            background: connectionState === "streaming" ? "rgba(34, 197, 94, 0.16)" : "rgba(148, 163, 184, 0.16)",
            color: connectionState === "streaming" ? "#bbf7d0" : "#cbd5e1",
            fontWeight: 700,
            whiteSpace: "nowrap"
          }}
        >
          {connectionState.toUpperCase()}
        </span>
      </div>

      <div style={{ display: "flex", gap: "12px", marginTop: "20px", flexWrap: "wrap" }}>
        <button
          disabled={isStreaming}
          onClick={startStreaming}
          style={{
            border: 0,
            borderRadius: "14px",
            padding: "13px 18px",
            background: isStreaming ? "#475569" : "#2563eb",
            color: "#ffffff",
            cursor: isStreaming ? "not-allowed" : "pointer",
            fontWeight: 700
          }}
        >
          Start microphone
        </button>
        <button
          disabled={!isStreaming}
          onClick={stopStreaming}
          style={{
            border: "1px solid rgba(248, 113, 113, 0.42)",
            borderRadius: "14px",
            padding: "13px 18px",
            background: "rgba(127, 29, 29, 0.22)",
            color: "#fecaca",
            cursor: !isStreaming ? "not-allowed" : "pointer",
            fontWeight: 700
          }}
        >
          Stop
        </button>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
          gap: "12px",
          marginTop: "20px"
        }}
      >
        <div style={{ padding: "16px", borderRadius: "16px", background: "rgba(2, 6, 23, 0.42)" }}>
          <p style={{ color: "#94a3b8", margin: "0 0 8px" }}>Chunks sent</p>
          <p style={{ fontSize: "28px", fontWeight: 800, margin: 0 }}>{chunksSent}</p>
        </div>
        <div style={{ padding: "16px", borderRadius: "16px", background: "rgba(2, 6, 23, 0.42)" }}>
          <p style={{ color: "#94a3b8", margin: "0 0 8px" }}>Backend acknowledgement</p>
          <p style={{ color: "#bfdbfe", margin: 0 }}>{lastAck}</p>
        </div>
        <div style={{ padding: "16px", borderRadius: "16px", background: "rgba(2, 6, 23, 0.42)" }}>
          <p style={{ color: "#94a3b8", margin: "0 0 8px" }}>Real transcripts saved</p>
          <p style={{ fontSize: "28px", fontWeight: 800, margin: 0 }}>{transcriptsCaptured}</p>
        </div>
      </div>

      <div style={{ marginTop: "18px", padding: "16px", borderRadius: "16px", background: "rgba(2, 6, 23, 0.42)" }}>
        <p style={{ color: "#94a3b8", margin: "0 0 8px" }}>
          Speech recognition: <strong style={{ color: "#bfdbfe" }}>{speechRecognitionState.toUpperCase()}</strong>
        </p>
        <p style={{ color: "#cbd5e1", margin: 0 }}>{speechMessage}</p>
        {interimTranscript ? (
          <p style={{ color: "#fef3c7", margin: "10px 0 0" }}>Hearing: {interimTranscript}</p>
        ) : null}
      </div>

      {errorMessage ? <p style={{ color: "#fecaca", marginBottom: 0 }}>{errorMessage}</p> : null}
    </article>
  );
}
