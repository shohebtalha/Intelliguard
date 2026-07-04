import React, { useState, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import { getAuditLogs } from '../api/api';

const S = {
    page: { display: 'flex', minHeight: '100vh', background: '#0f1117' },
    main: { marginLeft: 220, flex: 1, padding: 28 },
    header: { marginBottom: 24 },
    title: { fontSize: 20, fontWeight: 600, color: '#fff', marginBottom: 4 },
    sub: { fontSize: 13, color: '#8892a4' },
    card: { background: '#13161f', border: '1px solid #1e2235', borderRadius: 12, overflow: 'hidden' },
    table: { width: '100%', borderCollapse: 'collapse' },
    th: { padding: '12px 16px', textAlign: 'left', fontSize: 11, color: '#8892a4', textTransform: 'uppercase', letterSpacing: 0.5, borderBottom: '1px solid #1e2235', background: '#0f1117' },
    td: { padding: '13px 16px', fontSize: 13, color: '#e2e8f0', borderBottom: '1px solid #1a1d27' },
    pill: (status) => ({ fontSize: 10, padding: '3px 10px', borderRadius: 20, fontWeight: 600, display: 'inline-block', background: status === 'BLOCK' ? '#2d1515' : status === 'REVIEW' ? '#2d2415' : '#152d15', color: status === 'BLOCK' ? '#fc8181' : status === 'REVIEW' ? '#f6ad55' : '#68d391' }),
    score: (score) => ({ fontWeight: 600, color: score > 0.7 ? '#fc8181' : score > 0.4 ? '#f6ad55' : '#68d391' }),
    modelBadge: { fontSize: 10, padding: '2px 8px', borderRadius: 4, background: '#1a1d27', color: '#8892a4', fontFamily: 'monospace' },
    empty: { textAlign: 'center', padding: 40, color: '#8892a4', fontSize: 13 },
};

export default function AuditPage() {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        getAuditLogs()
            .then(res => setLogs(res.data.data || []))
            .catch(() => {})
            .finally(() => setLoading(false));
    }, []);

    return (
        <div style={S.page}>
            <Sidebar />
            <div style={S.main}>
                <div style={S.header}>
                    <div style={S.title}>Audit Log</div>
                    <div style={S.sub}>Immutable record of every fraud decision — {logs.length} entries</div>
                </div>

                <div style={S.card}>
                    <table style={S.table}>
                        <thead>
                        <tr>
                            {['Timestamp', 'Transaction ID', 'Sender', 'Amount', 'Decision', 'Score', 'Model', 'Time', 'By'].map(h => (
                                <th key={h} style={S.th}>{h}</th>
                            ))}
                        </tr>
                        </thead>
                        <tbody>
                        {logs.map(log => (
                            <tr key={log.id}>
                                <td style={{...S.td, fontSize: 11, color: '#8892a4'}}>
                                    {log.createdAt ? new Date(log.createdAt).toLocaleString() : '--'}
                                </td>
                                <td style={{...S.td, fontFamily: 'monospace', fontSize: 11, color: '#8892a4'}}>
                                    {log.transactionId?.slice(0, 8)}...
                                </td>
                                <td style={S.td}>{log.senderId}</td>
                                <td style={S.td}>₹{Number(log.amount).toLocaleString()}</td>
                                <td style={S.td}><span style={S.pill(log.decision)}>{log.decision}</span></td>
                                <td style={{...S.td, ...S.score(log.fraudScore)}}>{log.fraudScore}</td>
                                <td style={S.td}><span style={S.modelBadge}>{log.modelVersion}</span></td>
                                <td style={S.td}>{log.decisionTimeMs}ms</td>
                                <td style={{...S.td, color: '#8892a4'}}>{log.performedBy}</td>
                            </tr>
                        ))}
                        {!loading && logs.length === 0 && (
                            <tr><td colSpan={9} style={S.empty}>No audit records yet</td></tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}