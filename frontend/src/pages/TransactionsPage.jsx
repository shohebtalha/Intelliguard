import React, { useState, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import { getTransactions } from '../api/api';

const S = {
    page: { display: 'flex', minHeight: '100vh', background: '#0f1117' },
    main: { marginLeft: 220, flex: 1, padding: 28 },
    header: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 },
    title: { fontSize: 20, fontWeight: 600, color: '#fff' },
    filters: { display: 'flex', gap: 10 },
    filterBtn: (active) => ({ padding: '6px 16px', borderRadius: 20, border: '1px solid', fontSize: 12, fontWeight: 500, cursor: 'pointer', background: active ? '#e53e3e' : 'transparent', borderColor: active ? '#e53e3e' : '#1e2235', color: active ? '#fff' : '#8892a4' }),
    card: { background: '#13161f', border: '1px solid #1e2235', borderRadius: 12, overflow: 'hidden' },
    table: { width: '100%', borderCollapse: 'collapse' },
    th: { padding: '12px 16px', textAlign: 'left', fontSize: 11, color: '#8892a4', textTransform: 'uppercase', letterSpacing: 0.5, borderBottom: '1px solid #1e2235', background: '#0f1117' },
    td: { padding: '13px 16px', fontSize: 13, color: '#e2e8f0', borderBottom: '1px solid #1a1d27' },
    pill: (status) => ({ fontSize: 10, padding: '3px 10px', borderRadius: 20, fontWeight: 600, display: 'inline-block', background: status === 'BLOCK' ? '#2d1515' : status === 'REVIEW' ? '#2d2415' : '#152d15', color: status === 'BLOCK' ? '#fc8181' : status === 'REVIEW' ? '#f6ad55' : '#68d391' }),
    score: (score) => ({ fontWeight: 600, color: score > 0.7 ? '#fc8181' : score > 0.4 ? '#f6ad55' : '#68d391' }),
    empty: { textAlign: 'center', padding: 40, color: '#8892a4', fontSize: 13 },
    search: { background: '#0f1117', border: '1px solid #1e2235', borderRadius: 8, padding: '8px 14px', color: '#e2e8f0', fontSize: 13, outline: 'none', width: 240 },
};

const FILTERS = ['ALL', 'APPROVE', 'REVIEW', 'BLOCK'];

export default function TransactionsPage() {
    const [transactions, setTransactions] = useState([]);
    const [filter, setFilter] = useState('ALL');
    const [search, setSearch] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => { fetchAll(); }, []);

    const fetchAll = async () => {
        setLoading(true);
        try {
            const res = await getTransactions();
            const payload = res.data.data;
            setTransactions(Array.isArray(payload) ? payload : payload?.content || []);
        } catch (err) {
            console.error('Failed to fetch transactions', err);
            setTransactions([]);
        } finally { setLoading(false); }
    };

    const filtered = transactions.filter(t => {
        const matchFilter = filter === 'ALL' || t.status === filter;
        const matchSearch = !search || t.senderId?.toLowerCase().includes(search.toLowerCase()) || t.id?.toLowerCase().includes(search.toLowerCase());
        return matchFilter && matchSearch;
    });

    return (
        <div style={S.page}>
            <Sidebar />
            <div style={S.main}>
                <div style={S.header}>
                    <div style={S.title}>Transactions</div>
                    <div style={S.filters}>
                        <input style={S.search} placeholder="Search sender ID..." value={search} onChange={e => setSearch(e.target.value)} />
                        {FILTERS.map(f => (
                            <button key={f} style={S.filterBtn(filter === f)} onClick={() => setFilter(f)}>{f}</button>
                        ))}
                    </div>
                </div>

                <div style={S.card}>
                    <table style={S.table}>
                        <thead>
                        <tr>
                            {['Transaction ID', 'Sender', 'Amount', 'Country', 'Method', 'Status', 'Score', 'Time', 'Reason'].map(h => (
                                <th key={h} style={S.th}>{h}</th>
                            ))}
                        </tr>
                        </thead>
                        <tbody>
                        {filtered.map(t => (
                            <tr key={t.id}>
                                <td style={{...S.td, fontFamily: 'monospace', fontSize: 11, color: '#8892a4'}}>{t.id?.slice(0, 8)}...</td>
                                <td style={S.td}>{t.senderId}</td>
                                <td style={S.td}>₹{Number(t.amount).toLocaleString()}</td>
                                <td style={S.td}>{t.country}</td>
                                <td style={S.td}>{t.paymentMethod}</td>
                                <td style={S.td}><span style={S.pill(t.status)}>{t.status}</span></td>
                                <td style={{...S.td, ...S.score(t.fraudScore)}}>{t.fraudScore}</td>
                                <td style={S.td}>{t.decisionTimeMs ? `${t.decisionTimeMs}ms` : '--'}</td>
                                <td style={{...S.td, fontSize: 11, color: '#8892a4', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap'}}>{t.flagReason || '—'}</td>
                            </tr>
                        ))}
                        {!loading && filtered.length === 0 && (
                            <tr><td colSpan={9} style={S.empty}>No transactions found</td></tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
