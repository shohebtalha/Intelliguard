import React, { useState, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import { getTransactions, submitTransaction, explainTransaction } from '../api/api';

const S = {
    page: { display: 'flex', minHeight: '100vh', background: '#0f1117' },
    main: { marginLeft: 220, flex: 1, padding: 28 },
    header: { marginBottom: 28 },
    title: { fontSize: 20, fontWeight: 600, color: '#fff', marginBottom: 4 },
    sub: { fontSize: 13, color: '#8892a4' },
    metrics: { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 28 },
    metric: { background: '#13161f', border: '1px solid #1e2235', borderRadius: 12, padding: '18px 20px' },
    metricLabel: { fontSize: 11, color: '#8892a4', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 8 },
    metricValue: { fontSize: 26, fontWeight: 600, color: '#fff', marginBottom: 4 },
    metricSub: (color) => ({ fontSize: 12, color: color || '#8892a4' }),
    grid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 },
    card: { background: '#13161f', border: '1px solid #1e2235', borderRadius: 12, padding: 20 },
    cardTitle: { fontSize: 13, fontWeight: 500, color: '#fff', marginBottom: 16 },
    txRow: { display: 'flex', alignItems: 'center', gap: 12, padding: '10px 0', borderBottom: '1px solid #1e2235' },
    avatar: (color) => ({ width: 32, height: 32, borderRadius: '50%', background: color, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 600, color: '#fff', flexShrink: 0 }),
    txInfo: { flex: 1, minWidth: 0 },
    txName: { fontSize: 13, fontWeight: 500, color: '#e2e8f0', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' },
    txMeta: { fontSize: 11, color: '#8892a4', marginTop: 2 },
    txAmount: { fontSize: 13, fontWeight: 600, color: '#e2e8f0', flexShrink: 0 },
    pill: (status) => ({
        fontSize: 10, padding: '2px 8px', borderRadius: 20, fontWeight: 600, flexShrink: 0,
        background: status === 'BLOCK' ? '#2d1515' : status === 'REVIEW' ? '#2d2415' : '#152d15',
        color: status === 'BLOCK' ? '#fc8181' : status === 'REVIEW' ? '#f6ad55' : '#68d391',
    }),
    form: { display: 'flex', flexDirection: 'column', gap: 10 },
    row: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 },
    label: { fontSize: 11, color: '#8892a4', marginBottom: 4, display: 'block' },
    input: { width: '100%', background: '#0f1117', border: '1px solid #1e2235', borderRadius: 8, padding: '8px 12px', color: '#e2e8f0', fontSize: 13, outline: 'none' },
    select: { width: '100%', background: '#0f1117', border: '1px solid #1e2235', borderRadius: 8, padding: '8px 12px', color: '#e2e8f0', fontSize: 13, outline: 'none' },
    btn: { background: '#e53e3e', color: '#fff', border: 'none', borderRadius: 8, padding: '10px', fontSize: 13, fontWeight: 600, cursor: 'pointer', marginTop: 4 },
    result: (status) => ({ padding: '12px 14px', borderRadius: 8, marginTop: 12, background: status === 'BLOCK' ? '#2d1515' : status === 'REVIEW' ? '#2d2415' : '#152d15', border: `1px solid ${status === 'BLOCK' ? '#e53e3e' : status === 'REVIEW' ? '#d69e2e' : '#38a169'}` }),
    resultTitle: { fontSize: 13, fontWeight: 600, color: '#fff', marginBottom: 6 },
    resultDetail: { fontSize: 12, color: '#8892a4', marginBottom: 2 },
    shap: { marginTop: 12 },
    shapBar: (pct) => ({ height: 6, borderRadius: 3, background: pct > 0.5 ? '#e53e3e' : pct > 0.2 ? '#d69e2e' : '#38a169', width: `${Math.min(pct * 100, 100)}%` }),
    shapTrack: { height: 6, background: '#1e2235', borderRadius: 3, marginTop: 4, overflow: 'hidden' },
};

const COLORS = ['#e53e3e', '#3182ce', '#38a169', '#d69e2e', '#805ad5'];

export default function DashboardPage() {
    const [transactions, setTransactions] = useState([]);
    const [form, setForm] = useState({ senderId: 'USER_001', receiverId: 'USER_002', amount: '5000', currency: 'INR', country: 'IN', paymentMethod: 'UPI', deviceType: 'MOBILE', ipAddress: '192.168.1.1' });
    const [result, setResult] = useState(null);
    const [explanation, setExplanation] = useState(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => { fetchTransactions(); }, []);

    const fetchTransactions = async () => {
        try {
            const res = await getTransactions();
            const payload = res.data.data;
            setTransactions(payload?.content || payload || []);
        } catch {}
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setResult(null);
        setExplanation(null);
        try {
            const res = await submitTransaction({ ...form, amount: parseFloat(form.amount) });
            const txn = res.data.data;
            setResult(txn);
            fetchTransactions();
            // Auto-fetch explanation
            try {
                const exp = await explainTransaction(txn.id);
                setExplanation(exp.data.data);
            } catch {}
        } catch (err) {
            setResult({ error: err.response?.data?.message || 'Error' });
        } finally { setLoading(false); }
    };

    const approved = transactions.filter(t => t.status === 'APPROVE').length;
    const blocked = transactions.filter(t => t.status === 'BLOCK').length;
    const review = transactions.filter(t => t.status === 'REVIEW').length;
    const avgTime = transactions.length
        ? Math.round(transactions.filter(t => t.decisionTimeMs).reduce((a, t) => a + (t.decisionTimeMs || 0), 0) / transactions.length)
        : 0;

    return (
        <div style={S.page}>
            <Sidebar />
            <div style={S.main}>
                <div style={S.header}>
                    <div style={S.title}>Fraud Detection Dashboard</div>
                    <div style={S.sub}>Real-time transaction analysis powered by XGBoost + Rule Engine</div>
                </div>

                <div style={S.metrics}>
                    {[
                        { label: 'Total Transactions', value: transactions.length, sub: 'All time', color: '#8892a4' },
                        { label: 'Blocked', value: blocked, sub: `${transactions.length ? ((blocked/transactions.length)*100).toFixed(1) : 0}% fraud rate`, color: '#fc8181' },
                        { label: 'Under Review', value: review, sub: 'Manual check needed', color: '#f6ad55' },
                        { label: 'Avg Decision Time', value: avgTime ? `${avgTime}ms` : '--', sub: 'Target < 100ms', color: avgTime < 100 ? '#68d391' : '#fc8181' },
                    ].map(({ label, value, sub, color }) => (
                        <div key={label} style={S.metric}>
                            <div style={S.metricLabel}>{label}</div>
                            <div style={S.metricValue}>{value}</div>
                            <div style={S.metricSub(color)}>{sub}</div>
                        </div>
                    ))}
                </div>

                <div style={S.grid}>
                    {/* Recent Transactions */}
                    <div style={S.card}>
                        <div style={S.cardTitle}>Recent Transactions</div>
                        {transactions.slice(0, 6).map((t, i) => (
                            <div key={t.id} style={S.txRow}>
                                <div style={S.avatar(COLORS[i % COLORS.length])}>
                                    {t.senderId?.slice(0, 2).toUpperCase()}
                                </div>
                                <div style={S.txInfo}>
                                    <div style={S.txName}>{t.senderId}</div>
                                    <div style={S.txMeta}>{t.paymentMethod} · {t.country}</div>
                                </div>
                                <div style={S.txAmount}>₹{Number(t.amount).toLocaleString()}</div>
                                <div style={S.pill(t.status)}>{t.status}</div>
                            </div>
                        ))}
                        {transactions.length === 0 && (
                            <div style={{ color: '#8892a4', fontSize: 13, textAlign: 'center', padding: 20 }}>
                                No transactions yet. Submit one →
                            </div>
                        )}
                    </div>

                    {/* Submit Transaction */}
                    <div style={S.card}>
                        <div style={S.cardTitle}>Submit Transaction for Analysis</div>
                        <form style={S.form} onSubmit={handleSubmit}>
                            <div style={S.row}>
                                <div>
                                    <label style={S.label}>Sender ID</label>
                                    <input style={S.input} value={form.senderId} onChange={e => setForm({...form, senderId: e.target.value})} />
                                </div>
                                <div>
                                    <label style={S.label}>Amount (₹)</label>
                                    <input style={S.input} value={form.amount} onChange={e => setForm({...form, amount: e.target.value})} />
                                </div>
                            </div>
                            <div style={S.row}>
                                <div>
                                    <label style={S.label}>Country</label>
                                    <select style={S.select} value={form.country} onChange={e => setForm({...form, country: e.target.value})}>
                                        <option value="IN">India (IN) — Safe</option>
                                        <option value="NG">Nigeria (NG) — High Risk</option>
                                        <option value="US">USA (US) — Safe</option>
                                        <option value="KP">North Korea (KP) — Blocked</option>
                                        <option value="PK">Pakistan (PK) — High Risk</option>
                                    </select>
                                </div>
                                <div>
                                    <label style={S.label}>Device</label>
                                    <select style={S.select} value={form.deviceType} onChange={e => setForm({...form, deviceType: e.target.value})}>
                                        <option value="MOBILE">Mobile</option>
                                        <option value="DESKTOP">Desktop</option>
                                        <option value="UNKNOWN">Unknown</option>
                                    </select>
                                </div>
                            </div>
                            <div style={S.row}>
                                <div>
                                    <label style={S.label}>Payment Method</label>
                                    <select style={S.select} value={form.paymentMethod} onChange={e => setForm({...form, paymentMethod: e.target.value})}>
                                        <option value="UPI">UPI</option>
                                        <option value="CARD">Card</option>
                                        <option value="NET_BANKING">Net Banking</option>
                                    </select>
                                </div>
                                <div>
                                    <label style={S.label}>Receiver ID</label>
                                    <input style={S.input} value={form.receiverId} onChange={e => setForm({...form, receiverId: e.target.value})} />
                                </div>
                            </div>
                            <button style={S.btn} type="submit" disabled={loading}>
                                {loading ? 'Analyzing...' : '⚡ Analyze Transaction'}
                            </button>
                        </form>

                        {result && !result.error && (
                            <div style={S.result(result.status)}>
                                <div style={S.resultTitle}>
                                    {result.status === 'BLOCK' ? '🚫 BLOCKED' : result.status === 'REVIEW' ? '⚠️ REVIEW' : '✅ APPROVED'}
                                    {' '}— Score: {result.fraudScore}
                                </div>
                                <div style={S.resultDetail}>Decision time: {result.decisionTimeMs}ms</div>
                                {result.flagReason && <div style={S.resultDetail}>Reason: {result.flagReason}</div>}

                                {explanation?.shapContributions && (
                                    <div style={S.shap}>
                                        <div style={{ fontSize: 11, color: '#8892a4', marginBottom: 8, marginTop: 8 }}>SHAP — Why was this flagged?</div>
                                        {Object.entries(explanation.shapContributions).slice(0, 4).map(([key, val]) => (
                                            <div key={key} style={{ marginBottom: 8 }}>
                                                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#8892a4', marginBottom: 2 }}>
                                                    <span>{key.replace(/_/g, ' ')}</span>
                                                    <span>{val}</span>
                                                </div>
                                                <div style={S.shapTrack}><div style={S.shapBar(val)} /></div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
