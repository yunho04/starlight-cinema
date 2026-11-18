import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { api, tokenStore } from './api/client.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [member, setMember] = useState(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (!tokenStore.get()) {
      setReady(true);
      return;
    }
    api
      .me()
      .then(setMember)
      .catch(() => tokenStore.clear())
      .finally(() => setReady(true));
  }, []);

  const login = useCallback(async (loginId, password) => {
    const { token, member } = await api.login(loginId, password);
    tokenStore.set(token);
    setMember(member);
    return member;
  }, []);

  const logout = useCallback(() => {
    tokenStore.clear();
    setMember(null);
  }, []);

  return <AuthContext.Provider value={{ member, ready, login, logout }}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);

export function RequireAuth({ children }) {
  const { member, ready } = useAuth();
  const location = useLocation();
  if (!ready) return null;
  if (!member) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return children;
}
