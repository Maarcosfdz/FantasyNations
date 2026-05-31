import type { Metadata } from "next";
import "./globals.css";
import { Toaster } from "sonner";
import { QueryProvider } from "@/shared/components/QueryProvider";
import { I18nProvider } from "@/shared/i18n/I18nProvider";

export const metadata: Metadata = {
  title: "Fantasy Nations",
  description:
    "Private fantasy football for friend leagues. Build your squad, compete in rankings and follow the action.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full">
      <body className="min-h-full flex flex-col antialiased">
        <QueryProvider>
          <I18nProvider>
            {children}
            <Toaster richColors position="top-right" />
          </I18nProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
