import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { transactionApi } from '../api/transactionApi';
import { TransactionReceiptModal } from '../components/TransactionReceiptModal';
import { FileText, Filter, ArrowUpRight, ArrowDownLeft, ChevronLeft, ChevronRight, RotateCcw } from 'lucide-react';

export const History = () => {
  const { user, account } = useAuth();

  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeReceipt, setActiveReceipt] = useState(null);

  // Filters
  const [type, setType] = useState('ALL');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const fetchTransactions = async () => {
    setLoading(true);
    try {
      const data = await transactionApi.getTransactions({
        type,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        page,
        size: 10,
      });

      setTransactions(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load transaction history:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [type, startDate, endDate, page]);

  const handleResetFilters = () => {
    setType('ALL');
    setStartDate('');
    setEndDate('');
    setPage(0);
  };

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '30px 24px' }}>
      {/* Title Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.85rem', fontWeight: 800, color: '#ffffff', marginBottom: '4px' }}>
            Transaction History
          </h2>
          <div style={{ fontSize: '0.875rem', color: '#94a3b8' }}>
            Immutable double-entry ledger records for account {account?.maskedAccountNumber}
          </div>
        </div>

        <div className="badge badge-role">
          {totalElements} Total Entries Recorded
        </div>
      </div>

      {/* Filter Toolbar */}
      <div
        className="glass-panel"
        style={{
          padding: '18px 24px',
          marginBottom: '24px',
          display: 'flex',
          alignItems: 'center',
          gap: '16px',
          flexWrap: 'wrap',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Filter size={16} color="#06b6d4" />
          <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase' }}>
            Filters:
          </span>
        </div>

        {/* Type Filter */}
        <select
          value={type}
          onChange={(e) => {
            setType(e.target.value);
            setPage(0);
          }}
          className="form-select"
          style={{ width: 'auto', minWidth: '130px', padding: '8px 12px', fontSize: '0.85rem' }}
        >
          <option value="ALL">All Types</option>
          <option value="DEBIT">Debits (Sent)</option>
          <option value="CREDIT">Credits (Received)</option>
        </select>

        {/* Start Date */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ fontSize: '0.8rem', color: '#64748b' }}>From:</span>
          <input
            type="date"
            value={startDate}
            onChange={(e) => {
              setStartDate(e.target.value);
              setPage(0);
            }}
            className="form-input"
            style={{ width: 'auto', padding: '7px 10px', fontSize: '0.85rem' }}
          />
        </div>

        {/* End Date */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ fontSize: '0.8rem', color: '#64748b' }}>To:</span>
          <input
            type="date"
            value={endDate}
            onChange={(e) => {
              setEndDate(e.target.value);
              setPage(0);
            }}
            className="form-input"
            style={{ width: 'auto', padding: '7px 10px', fontSize: '0.85rem' }}
          />
        </div>

        {/* Reset */}
        <button
          onClick={handleResetFilters}
          className="btn btn-secondary"
          style={{ padding: '7px 12px', fontSize: '0.8rem', marginLeft: 'auto' }}
        >
          <RotateCcw size={14} />
          Reset
        </button>
      </div>

      {/* Transactions Table */}
      <div className="glass-panel" style={{ overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
            <thead>
              <tr
                style={{
                  background: 'rgba(0, 0, 0, 0.4)',
                  borderBottom: '1px solid var(--border-subtle)',
                  color: '#94a3b8',
                  fontSize: '0.75rem',
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                }}
              >
                <th style={{ padding: '14px 20px' }}>Type</th>
                <th style={{ padding: '14px 20px' }}>Reference ID</th>
                <th style={{ padding: '14px 20px' }}>Counterparty</th>
                <th style={{ padding: '14px 20px' }}>Date &amp; Time</th>
                <th style={{ padding: '14px 20px', textAlign: 'right' }}>Amount</th>
                <th style={{ padding: '14px 20px', textAlign: 'right' }}>Resulting Balance</th>
                <th style={{ padding: '14px 20px', textAlign: 'center' }}>Receipt</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: '#94a3b8' }}>
                    Loading transactions...
                  </td>
                </tr>
              ) : transactions.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: '#64748b' }}>
                    No transactions match the selected filters.
                  </td>
                </tr>
              ) : (
                transactions.map((tx) => {
                  const isDebit = tx.entryType === 'DEBIT';
                  const dateStr = new Date(tx.timestamp).toLocaleString('en-US', {
                    month: 'short',
                    day: 'numeric',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  });

                  return (
                    <tr
                      key={tx.id}
                      style={{
                        borderBottom: '1px solid var(--border-subtle)',
                        transition: 'background 0.15s ease',
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(255, 255, 255, 0.03)')}
                      onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                    >
                      {/* Type Badge */}
                      <td style={{ padding: '16px 20px' }}>
                        <span className={`badge ${isDebit ? 'badge-debit' : 'badge-credit'}`}>
                          {isDebit ? <ArrowUpRight size={13} /> : <ArrowDownLeft size={13} />}
                          {tx.entryType}
                        </span>
                      </td>

                      {/* Reference ID */}
                      <td style={{ padding: '16px 20px', fontFamily: 'monospace', color: '#06b6d4', fontWeight: 600 }}>
                        {tx.referenceId}
                      </td>

                      {/* Counterparty */}
                      <td style={{ padding: '16px 20px' }}>
                        <div style={{ fontWeight: 600, color: '#f8fafc' }}>{tx.counterpartyName}</div>
                        <div style={{ fontSize: '0.75rem', color: '#64748b', fontFamily: 'monospace' }}>
                          {tx.counterpartyMaskedAccountNumber}
                        </div>
                      </td>

                      {/* Timestamp */}
                      <td style={{ padding: '16px 20px', color: '#94a3b8', fontSize: '0.85rem' }}>
                        {dateStr}
                      </td>

                      {/* Amount */}
                      <td style={{ padding: '16px 20px', textAlign: 'right', fontWeight: 700, color: isDebit ? '#fb7185' : '#34d399' }}>
                        {isDebit ? '-' : '+'}${tx.amount?.toFixed(2)}
                      </td>

                      {/* Resulting Balance */}
                      <td style={{ padding: '16px 20px', textAlign: 'right', fontWeight: 600, color: '#cbd5e1' }}>
                        ${tx.resultingBalance?.toFixed(2)}
                      </td>

                      {/* Action */}
                      <td style={{ padding: '16px 20px', textAlign: 'center' }}>
                        <button
                          onClick={() =>
                            setActiveReceipt({
                              referenceId: tx.referenceId,
                              type: tx.transferType,
                              status: tx.status,
                              amount: tx.amount,
                              currency: tx.currency,
                              senderName: isDebit ? user.fullName : tx.counterpartyName,
                              recipientName: isDebit ? tx.counterpartyName : user.fullName,
                              sourceMaskedAccountNumber: isDebit ? account.maskedAccountNumber : tx.counterpartyMaskedAccountNumber,
                              destinationMaskedAccountNumber: isDebit ? tx.counterpartyMaskedAccountNumber : account.maskedAccountNumber,
                              description: tx.description,
                              resultingBalance: tx.resultingBalance,
                              timestamp: tx.timestamp,
                            })
                          }
                          className="btn btn-secondary"
                          style={{ padding: '4px 10px', fontSize: '0.75rem', borderRadius: '6px' }}
                        >
                          <FileText size={14} />
                          Receipt
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Toolbar */}
        {totalPages > 1 && (
          <div
            style={{
              padding: '16px 24px',
              borderTop: '1px solid var(--border-subtle)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              background: 'rgba(0, 0, 0, 0.25)',
            }}
          >
            <div style={{ fontSize: '0.85rem', color: '#94a3b8' }}>
              Showing Page <strong>{page + 1}</strong> of <strong>{totalPages}</strong>
            </div>

            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="btn btn-secondary"
                style={{ padding: '6px 12px', fontSize: '0.8rem' }}
              >
                <ChevronLeft size={16} />
                Previous
              </button>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="btn btn-secondary"
                style={{ padding: '6px 12px', fontSize: '0.8rem' }}
              >
                Next
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Receipt Modal */}
      <TransactionReceiptModal
        isOpen={!!activeReceipt}
        onClose={() => setActiveReceipt(null)}
        receipt={activeReceipt}
      />
    </div>
  );
};
