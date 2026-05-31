"use client";

import { Suspense, useRef } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import { Float, ContactShadows, Environment, PresentationControls } from "@react-three/drei";
import * as THREE from "three";

function GenericTrophy() {
  const group = useRef<THREE.Group>(null);

  // Slowly rotate on Y axis
  useFrame((_state, delta) => {
    if (group.current) {
      group.current.rotation.y += delta * 0.3;
    }
  });

  const goldMaterial = new THREE.MeshStandardMaterial({
    color: "#d4a017",
    metalness: 0.95,
    roughness: 0.1,
  });

  const darkGoldMaterial = new THREE.MeshStandardMaterial({
    color: "#a07808",
    metalness: 0.9,
    roughness: 0.2,
  });

  return (
    <group ref={group} position={[0, -0.5, 0]}>
      {/* Base platform */}
      <mesh material={darkGoldMaterial} position={[0, -1.4, 0]}>
        <cylinderGeometry args={[0.55, 0.65, 0.15, 32]} />
      </mesh>

      {/* Lower base ring */}
      <mesh material={darkGoldMaterial} position={[0, -1.25, 0]}>
        <cylinderGeometry args={[0.45, 0.55, 0.12, 32]} />
      </mesh>

      {/* Stem */}
      <mesh material={goldMaterial} position={[0, -0.65, 0]}>
        <cylinderGeometry args={[0.12, 0.18, 1.1, 20]} />
      </mesh>

      {/* Cup body */}
      <mesh material={goldMaterial} position={[0, 0.3, 0]}>
        <cylinderGeometry args={[0.55, 0.15, 1.2, 32, 1, true]} />
      </mesh>

      {/* Cup rim */}
      <mesh material={goldMaterial} position={[0, 0.95, 0]}>
        <torusGeometry args={[0.55, 0.06, 16, 32]} />
      </mesh>

      {/* Left handle */}
      <mesh material={darkGoldMaterial} position={[-0.72, 0.4, 0]} rotation={[0, 0, Math.PI / 6]}>
        <torusGeometry args={[0.18, 0.04, 12, 20, Math.PI]} />
      </mesh>

      {/* Right handle */}
      <mesh material={darkGoldMaterial} position={[0.72, 0.4, 0]} rotation={[0, 0, -Math.PI / 6]}>
        <torusGeometry args={[0.18, 0.04, 12, 20, Math.PI]} />
      </mesh>

      {/* Star on top */}
      <mesh material={goldMaterial} position={[0, 1.25, 0]}>
        <sphereGeometry args={[0.1, 16, 16]} />
      </mesh>
    </group>
  );
}

export function TrophyCanvas() {
  return (
    <div className="w-full h-[420px] md:h-[520px]">
      <Canvas camera={{ position: [0, 0, 4.5], fov: 45 }}>
        <ambientLight intensity={0.4} />
        <directionalLight position={[5, 5, 5]} intensity={1.2} castShadow />
        <pointLight position={[-3, 3, 3]} intensity={0.6} color="#fbbf24" />
        <Suspense fallback={null}>
          <Environment preset="city" />
          <PresentationControls
            global
            snap
            polar={[-0.2, 0.2]}
            azimuth={[-Infinity, Infinity]}
          >
            <Float floatIntensity={0.8} rotationIntensity={0.3} speed={2}>
              <GenericTrophy />
            </Float>
          </PresentationControls>
          <ContactShadows position={[0, -2.2, 0]} opacity={0.5} scale={4} blur={2} />
        </Suspense>
      </Canvas>
    </div>
  );
}
