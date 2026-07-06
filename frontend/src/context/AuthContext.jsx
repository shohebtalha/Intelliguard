import React, { createContext, useContext, useState, useEffect } from 'react';
import { login as loginApi, logoutSession } from '../api/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Check if user is already logged in (token in localStorage)
        const token = localStorage.getItem('token');
        const refreshToken = localStorage.getItem('refreshToken');
        const username = localStorage.getItem('username');
        const role = localStorage.getItem('role');
        const tenantId = localStorage.getItem('tenantId');
        if (token && username) {
            setUser({ token, refreshToken, username, role, tenantId });
        }
        setLoading(false);
    }, []);

    const login = async (username, password) => {
        const response = await loginApi(username, password);
        const { token, refreshToken, username: uname, role, tenantId } = response.data.data;
        localStorage.setItem('token', token);
        localStorage.setItem('refreshToken', refreshToken);
        localStorage.setItem('username', uname);
        localStorage.setItem('role', role);
        localStorage.setItem('tenantId', tenantId);
        setUser({ token, refreshToken, username: uname, role, tenantId });
        return response;
    };

    const logout = async () => {
        const refreshToken = localStorage.getItem('refreshToken');
        if (refreshToken) {
            try {
                await logoutSession(refreshToken);
            } catch {}
        }
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('username');
        localStorage.removeItem('role');
        localStorage.removeItem('tenantId');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
