import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';
import { accountApi } from '../api/accountApi';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('banking_token'));
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('banking_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [account, setAccount] = useState(null);
  const [loading, setLoading] = useState(true);

  // Sync token and user with localStorage
  const handleAuthSuccess = (data) => {
    localStorage.setItem('banking_token', data.token);
    const userInfo = {
      userId: data.userId,
      username: data.username,
      email: data.email,
      fullName: `${data.firstName} ${data.lastName}`,
      role: data.role,
    };
    localStorage.setItem('banking_user', JSON.stringify(userInfo));
    setToken(data.token);
    setUser(userInfo);

    // Set initial account data
    setAccount({
      id: data.accountId,
      accountNumber: data.accountNumber,
      maskedAccountNumber: data.maskedAccountNumber,
      balance: data.balance,
      currency: data.currency,
    });
  };

  const login = async (credentials) => {
    const data = await authApi.login(credentials);
    handleAuthSuccess(data);
    return data;
  };

  const register = async (userData) => {
    const data = await authApi.register(userData);
    handleAuthSuccess(data);
    return data;
  };

  const logout = () => {
    localStorage.removeItem('banking_token');
    localStorage.removeItem('banking_user');
    setToken(null);
    setUser(null);
    setAccount(null);
  };

  const refreshAccount = async () => {
    if (!token) return;
    try {
      const data = await accountApi.getMyAccount();
      setAccount({
        id: data.accountId,
        accountNumber: data.accountNumber,
        maskedAccountNumber: data.maskedAccountNumber,
        balance: data.balance,
        currency: data.currency,
        status: data.status,
      });
    } catch (err) {
      console.error('Failed to refresh account data:', err);
    }
  };

  useEffect(() => {
    if (token) {
      refreshAccount().finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [token]);

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        account,
        isAuthenticated: !!token,
        loading,
        login,
        register,
        logout,
        refreshAccount,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
