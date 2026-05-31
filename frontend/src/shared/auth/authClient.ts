import apiClient from "../api/apiClient";

export interface AuthUser {
  userId: string;
  email: string;
  nickname: string;
  avatarUrl: string | null;
}

export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
  nickname: string;
  avatarUrl: string | null;
}

export async function register(
  email: string,
  nickname: string,
  password: string
): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>("/api/auth/register", {
    email,
    nickname,
    password,
  });
  persistAuth(data);
  return data;
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>("/api/auth/login", {
    email,
    password,
  });
  persistAuth(data);
  return data;
}

export async function forgotPassword(email: string): Promise<void> {
  await apiClient.post("/api/auth/forgot-password", { email });
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
  await apiClient.post("/api/auth/reset-password", { token, newPassword });
}

export function logout(): void {
  localStorage.removeItem("fn_token");
  localStorage.removeItem("fn_user");
}

export function getStoredUser(): AuthUser | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem("fn_user");
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function persistAuth(data: AuthResponse): void {
  localStorage.setItem("fn_token", data.token);
  const user: AuthUser = {
    userId: data.userId,
    email: data.email,
    nickname: data.nickname,
    avatarUrl: data.avatarUrl,
  };
  localStorage.setItem("fn_user", JSON.stringify(user));
}
