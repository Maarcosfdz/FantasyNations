"use client";

import { useState } from "react";
import { LandingHeader } from "./landing/LandingHeader";
import { HeroSection } from "./landing/HeroSection";
import { DescriptionSection } from "./landing/DescriptionSection";
import { AboutSection } from "./landing/AboutSection";
import { SmoothScrollProvider } from "./landing/SmoothScrollProvider";
import { AuthModal } from "@/shared/auth/AuthModal";

export default function LandingPage() {
  const [authOpen, setAuthOpen] = useState(false);

  return (
    <SmoothScrollProvider>
      <main className="bg-white">
        <LandingHeader onLoginClick={() => setAuthOpen(true)} />
        <HeroSection onLoginClick={() => setAuthOpen(true)} />
        <DescriptionSection />
        <AboutSection />
        <AuthModal open={authOpen} onClose={() => setAuthOpen(false)} />
      </main>
    </SmoothScrollProvider>
  );
}
