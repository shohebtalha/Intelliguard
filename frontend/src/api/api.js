import axios from 'axios';

const BASE_URL = 'http://localhost:8080';

// Create axios instance with base URL
const api = axios.create({
    baseURL: BASE_URL,
    headers: { 'Content-Type': 'application/json' },
});

// Attach JWT token to every request automatically
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// If token expires, redirect to login
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// ─── Auth ────────────────────────────────────────────────
export const login = (username, password) =>
    api.post('/api/auth/login', { username, password });

export const register = (username, password, role) =>
    api.post('/api/auth/register', { username, password, role });

// ─── Transactions ────────────────────────────────────────
export const submitTransaction = (data) =>
    api.post('/api/transactions', data);

export const getTransactions = (status) =>
    api.get('/api/transactions', { params: status ? { status } : {} });

export const getTransactionById = (id) =>
    api.get(`/api/transactions/${id}`);

export const explainTransaction = (id) =>
    api.get(`/api/transactions/${id}/explain`);

// ─── Audit ───────────────────────────────────────────────
export const getAuditLogs = () =>
    api.get('/api/audit');

export const getAuditBySender = (senderId) =>
    api.get(`/api/audit/sender/${senderId}`);

export default api;