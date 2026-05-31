import { create } from "zustand";
import { AuthUser, getStoredUser, logout as clientLogout } from "./authClient";

interface UserStore {
  user: AuthUser | null;
  setUser: (user: AuthUser | null) => void;
  logout: () => void;
  hydrate: () => void;
}

export const useCurrentUser = create<UserStore>((set) => ({
  user: null,
  setUser: (user) => set({ user }),
  logout: () => {
    clientLogout();
    set({ user: null });
  },
  hydrate: () => {
    set({ user: getStoredUser() });
  },
}));
