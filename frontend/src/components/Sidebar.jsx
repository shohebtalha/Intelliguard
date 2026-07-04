import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const S = {
    sidebar: {
        width: 220, minHeight: '100vh', background: '#13161f',
        borderRight: '1px solid #1e2235', padding: '20px 12px',
        display: 'flex', flexDirection: 'column', position: 'fixed', top: 0, left: 0,
    },
    logo: {
        display: 'flex', alignItems: 'center', gap: 10,
        padding: '0 8px 24px', borderBottom: '1px solid #1e2235', marginBottom: 16,
    },
    logoIcon: {
        width: 32, height: 32, background: '#e53e3e', borderRadius: 8,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 14, fontWeight: 700, color: '#fff',
    },
    logoText: { fontSize: 15, fontWeight: 600, color: '#fff' },
    item: (active) => ({
        display: 'flex', alignItems: 'center', gap: 10,
        padding: '9px 12px', borderRadius: 8, marginBottom: 2, cursor: 'pointer',
        fontSize: 13, fontWeight: active ? 500 : 400,
        color: active ? '#fff' : '#8892a4',
        background: active ? '#1e2235' : 'transparent',
        transition: 'all 0.15s',
    }),
    icon: { fontSize: 16, width: 18, textAlign: 'center' },
    bottom: { marginTop: 'auto', paddingTop: 16, borderTop: '1px solid #1e2235' },
    user: { padding: '8px 12px', fontSize: 12, color: '#8892a4' },
    role: { fontSize: 10, color: '#e53e3e', fontWeight: 600, textTransform: 'uppercase', letterSpacing: 1 },
    logout: {
        display: 'flex', alignItems: 'center', gap: 10, padding: '9px 12px',
        borderRadius: 8, cursor: 'pointer', fontSize: 13, color: '#8892a4',
        marginTop: 4,
    },
};

const NAV = [
    { path: '/', icon: '▦', label: 'Dashboard' },
    { path: '/transactions', icon: '⇄', label: 'Transactions' },
    { path: '/audit', icon: '☰', label: 'Audit Log' },
];

export default function Sidebar() {
    const navigate = useNavigate();
    const location = useLocation();
    const { user, logout } = useAuth();

    return (
        <div style={S.sidebar}>
            <div style={S.logo}>
                <div style={S.logoIcon}>IG</div>
                <span style={S.logoText}>IntelliGuard</span>
            </div>

            <nav>
                {NAV.map(({ path, icon, label }) => (
                    <div key={path} style={S.item(location.pathname === path)}
                         onClick={() => navigate(path)}>
                        <span style={S.icon}>{icon}</span>
                        {label}
                    </div>
                ))}
            </nav>

            <div style={S.bottom}>
                <div style={S.user}>
                    <div style={{ color: '#e2e8f0', fontWeight: 500, marginBottom: 2 }}>
                        {user?.username}
                    </div>
                    <div style={S.role}>{user?.role}</div>
                </div>
                <div style={S.logout} onClick={logout}>
                    <span style={S.icon}>⇥</span> Logout
                </div>
            </div>
        </div>
    );
}