"use client";

import { useEffect, useRef } from "react";
import { motion } from "motion/react";
import { ChevronDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import dynamic from "next/dynamic";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";

const TrophyCanvas = dynamic(
  () => import("./TrophyCanvas").then((m) => m.TrophyCanvas),
  { ssr: false }
);

interface HeroSectionProps {
  onLoginClick: () => void;
}

export function HeroSection({ onLoginClick }: HeroSectionProps) {
  const sectionRef = useRef<HTMLElement>(null);
  const trophyWrapRef = useRef<HTMLDivElement>(null);
  const textRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (typeof window === "undefined") return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    gsap.registerPlugin(ScrollTrigger);

    const ctx = gsap.context(() => {
      // Subtle parallax: trophy drifts up faster than text as the user scrolls past the hero.
      gsap.to(trophyWrapRef.current, {
        y: -80,
        ease: "none",
        scrollTrigger: {
          trigger: sectionRef.current,
          start: "top top",
          end: "bottom top",
          scrub: true,
        },
      });
      gsap.to(textRef.current, {
        y: -30,
        opacity: 0.85,
        ease: "none",
        scrollTrigger: {
          trigger: sectionRef.current,
          start: "top top",
          end: "bottom top",
          scrub: true,
        },
      });
    }, sectionRef);

    return () => ctx.revert();
  }, []);

  return (
    <section
      id="home"
      ref={sectionRef}
      className="relative min-h-screen flex flex-col items-center justify-center overflow-hidden"
      style={{
        background:
          "radial-gradient(ellipse at 50% 0%, rgba(59,130,246,0.10) 0%, transparent 60%), linear-gradient(to bottom, #ffffff, #f4f4f5)",
      }}
    >
      {/* Stadium grid lines — very subtle on the light background */}
      <div
        className="absolute inset-0 opacity-[0.06] pointer-events-none"
        style={{
          backgroundImage:
            "repeating-linear-gradient(0deg, transparent, transparent 40px, rgba(15,23,42,0.6) 40px, rgba(15,23,42,0.6) 41px), repeating-linear-gradient(90deg, transparent, transparent 40px, rgba(15,23,42,0.6) 40px, rgba(15,23,42,0.6) 41px)",
        }}
      />

      <div className="relative z-10 max-w-5xl mx-auto px-4 flex flex-col md:flex-row items-center gap-8 pt-20">
        <div className="flex-1 text-center md:text-left">
          <motion.div
            ref={textRef}
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7 }}
          >
            <span className="inline-block bg-blue-50 text-blue-700 text-xs font-semibold px-3 py-1 rounded-full border border-blue-100 mb-4">
              Private Leagues · National Teams
            </span>
            <h1 className="text-5xl md:text-7xl font-extrabold text-zinc-900 leading-[1.05] mb-4 tracking-tight">
              Fantasy
              <br />
              <span className="bg-gradient-to-r from-amber-500 to-orange-500 bg-clip-text text-transparent">
                Nations
              </span>
            </h1>
            <p className="text-zinc-600 text-lg md:text-xl max-w-md mb-8 leading-relaxed">
              Build your dream squad, dominate private leagues, and prove you know
              football better than your friends.
            </p>
            <div className="flex flex-col sm:flex-row gap-3 justify-center md:justify-start">
              <Button
                size="lg"
                onClick={onLoginClick}
                className="bg-blue-600 hover:bg-blue-700 text-white font-semibold px-8"
              >
                Play Now
              </Button>
              <Button
                size="lg"
                variant="outline"
                onClick={() =>
                  document
                    .getElementById("about")
                    ?.scrollIntoView({ behavior: "smooth" })
                }
                className="border-zinc-300 text-zinc-700 hover:text-zinc-900"
              >
                Learn More
              </Button>
            </div>
          </motion.div>
        </div>

        <motion.div
          ref={trophyWrapRef}
          className="flex-1 w-full max-w-sm md:max-w-md"
          initial={{ opacity: 0, scale: 0.85 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.8, delay: 0.2 }}
        >
          <TrophyCanvas />
        </motion.div>
      </div>

      <motion.div
        className="absolute bottom-8 left-1/2 -translate-x-1/2 text-zinc-400"
        animate={{ y: [0, 8, 0] }}
        transition={{ repeat: Infinity, duration: 2 }}
      >
        <ChevronDown className="h-6 w-6" />
      </motion.div>
    </section>
  );
}
