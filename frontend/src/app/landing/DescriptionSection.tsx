"use client";

import { motion } from "motion/react";
import { ShoppingCart, Users, Trophy, TrendingUp } from "lucide-react";
import { useT } from "@/shared/i18n/I18nProvider";

const features = [
  {
    icon: Users,
    title: "Private Leagues",
    description:
      "Create leagues for your friends, invite them with a code and compete all tournament long.",
  },
  {
    icon: ShoppingCart,
    title: "Daily Market",
    description:
      "A fresh market appears every 24 hours. Buy the players you need before anyone else does.",
  },
  {
    icon: Trophy,
    title: "Release Clauses",
    description:
      "Pay a player's release clause to steal them from a rival's squad. Protect yours wisely.",
  },
  {
    icon: TrendingUp,
    title: "Rankings & Points",
    description:
      "Earn points from real match performances. Top the leaderboard and earn money for your squad.",
  },
];

export function DescriptionSection() {
  const t = useT();
  return (
    <section className="py-20 px-4 bg-zinc-50">
      <div className="max-w-5xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-14"
        >
          <h2 className="text-3xl md:text-4xl font-bold text-zinc-900 mb-4 tracking-tight">
            {t("landing.description.title")}
          </h2>
          <p className="text-zinc-600 max-w-xl mx-auto">
            {t("landing.description.body")}
          </p>
        </motion.div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {features.map((feature, i) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: i * 0.1 }}
              className="bg-white rounded-2xl p-6 border border-zinc-200 shadow-sm hover:shadow-md hover:-translate-y-0.5 hover:border-blue-300 transition-all"
            >
              <div className="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center mb-4">
                <feature.icon className="h-5 w-5 text-blue-600" />
              </div>
              <h3 className="font-semibold text-zinc-900 mb-2">{feature.title}</h3>
              <p className="text-zinc-600 text-sm leading-relaxed">
                {feature.description}
              </p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
