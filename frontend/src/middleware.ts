import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const PROTECTED_PREFIXES = ["/leagues", "/profile"];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  const isProtected = PROTECTED_PREFIXES.some((prefix) => pathname.startsWith(prefix));
  if (!isProtected) return NextResponse.next();

  // Token is stored in localStorage (client-side only).
  // Server-side auth check requires cookie-based tokens.
  // For MVP, redirect protection is handled client-side in each page.
  // This middleware can be extended to use httpOnly cookies later.
  return NextResponse.next();
}

export const config = {
  matcher: ["/leagues/:path*", "/profile/:path*"],
};
