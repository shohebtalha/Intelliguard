import React, { useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';
import { addCaseNote, assignCase, getCaseById, getCases, resolveCase } from '../api/api';
import { useAuth } from '../context/AuthContext';

const S = {
    page: { display: 'flex', minHeight: '100vh', background: '#0f1117' },
    main: { marginLeft: 220, flex: 1, padding: 28 },
    header: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 },
    title: { fontSize: 20, fontWeight: 600, color: '#fff' },
    filters: { display: 'flex', gap: 10 },
    filterBtn: (active) => ({ padding: '6px 14px', borderRadius: 8, border: '1px solid', fontSize: 12, cursor: 'pointer', background: active ? '#e53e3e' : 'transparent', borderColor: active ? '#e53e3e' : '#1e2235', color: active ? '#fff' : '#8892a4' }),
    grid: { display: 'grid', gridTemplateColumns: 'minmax(420px, 1fr) minmax(360px, 0.75fr)', gap: 16 },
    panel: { background: '#13161f', border: '1px solid #1e2235', borderRadius: 8, overflow: 'hidden' },
    row: (active) => ({ padding: 14, borderBottom: '1px solid #1e2235', cursor: 'pointer', background: active ? '#1e2235' : 'transparent' }),
    rowTop: { display: 'flex', justifyContent: 'space-between', gap: 12, marginBottom: 8 },
    caseId: { fontFamily: 'monospace', fontSize: 11, color: '#8892a4' },
    sender: { fontSize: 13, color: '#fff', fontWeight: 600 },
    meta: { fontSize: 12, color: '#8892a4', lineHeight: 1.5 },
    pill: (value) => ({ fontSize: 10, padding: '3px 8px', borderRadius: 999, fontWeight: 700, color: '#fff', background: value === 'HIGH' || value === 'BLOCK' ? '#e53e3e' : value === 'MEDIUM' || value === 'REVIEW' ? '#d69e2e' : value === 'RESOLVED' ? '#38a169' : '#3182ce' }),
    detail: { padding: 18 },
    detailTitle: { color: '#fff', fontSize: 15, fontWeight: 600, marginBottom: 14 },
    label: { fontSize: 11, color: '#8892a4', marginBottom: 5, display: 'block' },
    input: { width: '100%', background: '#0f1117', border: '1px solid #1e2235', borderRadius: 8, padding: '9px 12px', color: '#e2e8f0', fontSize: 13, outline: 'none', marginBottom: 10 },
    textarea: { width: '100%', minHeight: 76, background: '#0f1117', border: '1px solid #1e2235', borderRadius: 8, padding: '9px 12px', color: '#e2e8f0', fontSize: 13, outline: 'none', marginBottom: 10, resize: 'vertical' },
    actions: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 14 },
    btn: { background: '#e53e3e', color: '#fff', border: 'none', borderRadius: 8, padding: '10px 12px', fontSize: 13, fontWeight: 600, cursor: 'pointer' },
    secondary: { background: '#1e2235', color: '#e2e8f0', border: '1px solid #2b3146', borderRadius: 8, padding: '10px 12px', fontSize: 13, fontWeight: 600, cursor: 'pointer' },
    note: { borderTop: '1px solid #1e2235', padding: '10px 0' },
    empty: { color: '#8892a4', fontSize: 13, padding: 24, textAlign: 'center' },
};

const FILTERS = ['OPEN', 'INVESTIGATING', 'RESOLVED'];

export default function CasesPage() {
    const { user } = useAuth();
    const [cases, setCases] = useState([]);
    const [selected, setSelected] = useState(null);
    const [status, setStatus] = useState('OPEN');
    const [assignee, setAssignee] = useState('');
    const [note, setNote] = useState('');
    const [resolutionNote, setResolutionNote] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => { fetchCases(status); }, [status]);

    const fetchCases = async (nextStatus = status) => {
        setLoading(true);
        try {
            const res = await getCases({ status: nextStatus });
            const payload = res.data.data;
            const rows = payload?.content || [];
            setCases(rows);
            if (!selected && rows.length) await selectCase(rows[0].id);
        } finally {
            setLoading(false);
        }
    };

    const selectCase = async (id) => {
        const res = await getCaseById(id);
        setSelected(res.data.data);
        setAssignee(res.data.data.assignedTo || user?.username || '');
        setNote('');
        setResolutionNote('');
    };

    const handleAssign = async () => {
        if (!selected || !assignee.trim()) return;
        const res = await assignCase(selected.id, assignee.trim());
        setSelected(res.data.data);
        fetchCases();
    };

    const handleNote = async () => {
        if (!selected || !note.trim()) return;
        const res = await addCaseNote(selected.id, note.trim());
        setSelected(res.data.data);
        setNote('');
    };

    const handleResolve = async (resolution) => {
        if (!selected) return;
        const res = await resolveCase(selected.id, resolution, resolutionNote.trim());
        setSelected(res.data.data);
        fetchCases();
    };

    return (
        <div style={S.page}>
            <Sidebar />
            <main style={S.main}>
                <div style={S.header}>
                    <div style={S.title}>Case Management</div>
                    <div style={S.filters}>
                        {FILTERS.map(f => (
                            <button key={f} style={S.filterBtn(status === f)} onClick={() => setStatus(f)}>{f}</button>
                        ))}
                    </div>
                </div>

                <div style={S.grid}>
                    <section style={S.panel}>
                        {cases.map(item => (
                            <div key={item.id} style={S.row(selected?.id === item.id)} onClick={() => selectCase(item.id)}>
                                <div style={S.rowTop}>
                                    <div>
                                        <div style={S.sender}>{item.senderId}</div>
                                        <div style={S.caseId}>{item.id?.slice(0, 8)} / txn {item.transactionId?.slice(0, 8)}</div>
                                    </div>
                                    <div style={{ display: 'flex', gap: 6, alignItems: 'flex-start' }}>
                                        <span style={S.pill(item.priority)}>{item.priority}</span>
                                        <span style={S.pill(item.decision)}>{item.decision}</span>
                                    </div>
                                </div>
                                <div style={S.meta}>Score {item.fraudScore ?? '--'} · {item.assignedTo || 'Unassigned'}</div>
                                <div style={S.meta}>{item.reason || 'No reason captured'}</div>
                            </div>
                        ))}
                        {!loading && cases.length === 0 && <div style={S.empty}>No cases in this queue</div>}
                    </section>

                    <section style={S.panel}>
                        {selected ? (
                            <div style={S.detail}>
                                <div style={S.detailTitle}>Investigation Detail</div>
                                <div style={S.meta}>Status: {selected.status} · Priority: {selected.priority}</div>
                                <div style={{ ...S.meta, marginBottom: 14 }}>Reason: {selected.reason || 'No reason captured'}</div>

                                <label style={S.label}>Assigned analyst</label>
                                <input style={S.input} value={assignee} onChange={e => setAssignee(e.target.value)} />
                                <button style={S.secondary} onClick={handleAssign}>Assign</button>

                                <label style={{ ...S.label, marginTop: 16 }}>Investigation note</label>
                                <textarea style={S.textarea} value={note} onChange={e => setNote(e.target.value)} />
                                <button style={S.secondary} onClick={handleNote}>Add Note</button>

                                <label style={{ ...S.label, marginTop: 16 }}>Resolution note</label>
                                <textarea style={S.textarea} value={resolutionNote} onChange={e => setResolutionNote(e.target.value)} />
                                <div style={S.actions}>
                                    <button style={S.btn} onClick={() => handleResolve('CONFIRMED_FRAUD')}>Confirm Fraud</button>
                                    <button style={S.secondary} onClick={() => handleResolve('FALSE_POSITIVE')}>False Positive</button>
                                </div>

                                <div style={S.detailTitle}>Notes</div>
                                {(selected.notes || []).map(n => (
                                    <div key={n.id} style={S.note}>
                                        <div style={S.meta}>{n.createdBy} · {n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}</div>
                                        <div style={{ color: '#e2e8f0', fontSize: 13 }}>{n.note}</div>
                                    </div>
                                ))}
                                {(!selected.notes || selected.notes.length === 0) && <div style={S.meta}>No notes yet</div>}
                            </div>
                        ) : <div style={S.empty}>Select a case</div>}
                    </section>
                </div>
            </main>
        </div>
    );
}
