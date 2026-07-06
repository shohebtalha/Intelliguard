import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';// Create axios instance with base URL
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
        const requestUrl = error.config?.url || '';
        const isAuthRequest = requestUrl.includes('/api/auth/login')
            || requestUrl.includes('/api/auth/refresh');
        if (error.response?.status === 401 && !isAuthRequest) {
            localStorage.removeItem('token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// ─── Auth ────────────────────────────────────────────────
export const login = (username, password) =>
    api.post('/api/auth/login', { username, password });

export const refreshSession = (refreshToken) =>
    api.post('/api/auth/refresh', { refreshToken });

export const logoutSession = (refreshToken) =>
    api.post('/api/auth/logout', { refreshToken });

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

export const getCases = (params = {}) =>
    api.get('/api/cases', { params });

export const getCaseById = (id) =>
    api.get(`/api/cases/${id}`);

export const assignCase = (id, assignedTo) =>
    api.patch(`/api/cases/${id}/assign`, { assignedTo });

export const addCaseNote = (id, note) =>
    api.post(`/api/cases/${id}/notes`, { note });

export const resolveCase = (id, resolution, note) =>
    api.patch(`/api/cases/${id}/resolve`, { resolution, note });

export default api;
