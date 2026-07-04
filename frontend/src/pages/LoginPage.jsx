import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const S = {
    page: {
        minHeight: '100vh', background: '#0f1117',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
    },
    card: {
        background: '#13161f', border: '1px solid #1e2235',
        borderRadius: 16, padding: 40, width: '100%', maxWidth: 400,
    },
    logo: {
        display: 'flex', alignItems: 'center', gap: 12, marginBottom: 32,
        justifyContent: 'center',
    },
    logoIcon: {
        width: 40, height: 40, background: '#e53e3e', borderRadius: 10,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 16, fontWeight: 700, color: '#fff',
    },
    logoText: { fontSize: 20, fontWeight: 700, color: '#fff' },
    subtitle: { fontSize: 13, color: '#8892a4', textAlign: 'center', marginBottom: 32 },
    label: { fontSize: 12, color: '#8892a4', marginBottom: 6, display: 'block' },
    input: {
        width: '100%', background: '#0f1117', border: '1px solid #1e2235',
        borderRadius: 8, padding: '10px 14px', color: '#e2e8f0',
        fontSize: 14, marginBottom: 16, outline: 'none',
    },
    btn: {
        width: '100%', background: '#e53e3e', color: '#fff', border: 'none',
        borderRadius: 8, padding: '12px', fontSize: 14, fontWeight: 600,
        cursor: 'pointer', marginTop: 8,
    },
    error: {
        background: '#2d1515', border: '1px solid #e53e3e', borderRadius: 8,
        padding: '10px 14px', color: '#fc8181', fontSize: 13, marginBottom: 16,
    },
    hint: { fontSize: 12, color: '#8892a4', textAlign: 'center', marginTop: 16 },
};

export default function LoginPage() {
    const [username, setUsername] = useState('admin');
    const [password, setPassword] = useState('password123');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            await login(username, password);
            navigate('/');
        } catch (err) {
            setError(err.response?.data?.message || 'Invalid credentials');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={S.page}>
            <div style={S.card}>
                <div style={S.logo}>
                    <div style={S.logoIcon}>IG</div>
                    <span style={S.logoText}>IntelliGuard</span>
                </div>
                <p style={S.subtitle}>AI-Powered Fraud Detection Engine</p>

                {error && <div style={S.error}>{error}</div>}

                <form onSubmit={handleLogin}>
                    <label style={S.label}>Username</label>
                    <input style={S.input} value={username}
                           onChange={e => setUsername(e.target.value)} placeholder="username" />

                    <label style={S.label}>Password</label>
                    <input style={S.input} type="password" value={password}
                           onChange={e => setPassword(e.target.value)} placeholder="password" />

                    <button style={S.btn} type="submit" disabled={loading}>
                        {loading ? 'Signing in...' : 'Sign In'}
                    </button>
                </form>

                <p style={S.hint}>Default: admin / password123</p>
            </div>
        </div>
    );
}