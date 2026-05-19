"use client";

type DownloadPostmortemButtonProps = {
  title: string;
  content: string;
  createdAt: string;
};

export function DownloadPostmortemButton({ title, content, createdAt }: DownloadPostmortemButtonProps) {
  function downloadMarkdown() {
    const markdown = `${content.trim()}\n`;
    const blob = new Blob([markdown], { type: "text/markdown;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = `${slugify(title)}-${createdAt.slice(0, 10)}.md`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  return (
    <button
      type="button"
      onClick={downloadMarkdown}
      style={{
        border: "1px solid rgba(103, 232, 249, 0.42)",
        borderRadius: "999px",
        padding: "8px 12px",
        background: "rgba(8, 145, 178, 0.18)",
        color: "#a5f3fc",
        cursor: "pointer",
        fontWeight: 700
      }}
    >
      Download Markdown
    </button>
  );
}

function slugify(value: string) {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 80) || "postmortem";
}
