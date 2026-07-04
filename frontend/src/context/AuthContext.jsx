import React, { createContext, useContext, useState, useEffect } from 'react';
import { login as loginApi } from '../api/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Check if user is already logged in (token in localStorage)
        const token = localStorage.getItem('token');
        const username = localStorage.getItem('username');
        const role = localStorage.getItem('role');
        if (token && username) {
            setUser({ token, username, role });
        }
        setLoading(false);
    }, []);

    const login = async (username, password) => {
        const response = await loginApi(username, password);
        const { token, username: uname, role } = response.data.data;
        localStorage.setItem('token', token);
        localStorage.setItem('username', uname);
        localStorage.setItem('role', role);
        setUser({ token, username: uname, role });
        return response;
    };

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('username');
        localStorage.removeItem('role');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);