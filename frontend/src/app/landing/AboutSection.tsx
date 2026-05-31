"use client";

import { motion } from "motion/react";
import { ExternalLink } from "lucide-react";
import { useT } from "@/shared/i18n/I18nProvider";

export function AboutSection() {
  const t = useT();
  return (
    <section id="about" className="py-20 px-4 bg-white">
      <div className="max-w-3xl mx-auto text-center">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
        >
          <h2 className="text-3xl md:text-4xl font-bold text-zinc-900 mb-6 tracking-tight">
            {t("landing.about.title")}
          </h2>
          <p className="text-zinc-700 text-lg leading-relaxed mb-8">
            {t("landing.about.body")}
          </p>
          <p className="text-zinc-500 mb-10">
            Built with Next.js, Spring Boot and a lot of football passion.
          </p>

          <div className="flex justify-center gap-4 flex-wrap">
            <a
              href="https://github.com/Maarcosfdz"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-2 bg-white hover:bg-zinc-50 text-zinc-900 px-5 py-3 rounded-xl border border-zinc-200 shadow-sm transition"
            >
              <ExternalLink className="h-5 w-5" />
              <span>GitHub</span>
            </a>
            <a
              href="https://www.linkedin.com/in/marcos-romay-82b16036a/"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-5 py-3 rounded-xl transition"
            >
              <ExternalLink className="h-5 w-5" />
              <span>LinkedIn</span>
            </a>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
