import "./globals.css";

export const metadata = {
  title: "VoxOps Command Center",
  description: "Voice AI incident command platform"
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
