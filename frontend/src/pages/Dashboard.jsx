import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { MaskedAccountCard } from '../components/MaskedAccountCard';
import { TransferModal } from '../components/TransferModal';
import { DepositModal } from '../components/DepositModal';
import { TransactionReceiptModal } from '../components/TransactionReceiptModal';
import { transactionApi } from '../api/transactionApi';
import { ArrowUpRight, ArrowDownLeft, Shield, Clock, FileText, ChevronRight, Lock } from 'lucide-react';

export const Dashboard = ({ onNavigateToHistory }) => {
  const { user, account, refreshAccount } = useAuth();

  const [isTransferOpen, setIsTransferOpen] = useState(false);
  const [isDepositOpen, setIsDepositOpen] = useState(false);
  const [activeReceipt, setActiveReceipt] = useState(null);

  const [recentTransactions, setRecentTransactions] = useState([]);
  const [loadingTx, setLoadingTx] = useState(true);

  const loadRecentTransactions = async () => {
    try {
      const data = await transactionApi.getTransactions({ page: 0, size: 5 });
      setRecentTransactions(data.content || []);
    } catch (err) {
      console.error('Failed to load recent transactions:', err);
    } finally {
      setLoadingTx(false);
    }
  };

  useEffect(() => {
    loadRecentTransactions();
  }, []);

  const handleTransactionSuccess = async (receipt) => {
    await refreshAccount();
    await loadRecentTransactions();
    setActiveReceipt(receipt);
  };

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '30px 24px' }}>
      {/* Top Banner Notice */}
      <div
        style={{
          background: 'rgba(245, 158, 11, 0.08)',
          border: '1px solid rgba(245, 158, 11, 0.2)',
          padding: '12px 18px',
          borderRadius: '12px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '28px',
          flexWrap: 'wrap',
          gap: '12px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Shield size={18} color="#f59e0b" />
          <span style={{ fontSize: '0.85rem', color: '#fbbf24', fontWeight: 600 }}>
            Educational Banking Sandbox: Transfers use simulated funds with atomic ACID transactions and pessimistic locking.
          </span>
        </div>
        <span className="badge badge-simulated" style={{ fontSize: '0.65rem' }}>
          Simulated Engine v1.0
        </span>
      </div>

      {/* Grid Layout: Account Card & Stats */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
          gap: '24px',
          marginBottom: '32px',
        }}
      >
        {/* Main Bank Card */}
        <MaskedAccountCard
          account={account}
          onOpenTransfer={() => setIsTransferOpen(true)}
          onOpenDeposit={() => setIsDepositOpen(true)}
          onRefresh={refreshAccount}
        />

        {/* Security & Concurrency Guarantees Panel */}
        <div
          className="glass-panel"
          style={{
            padding: '28px',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
              <Lock size={18} color="#818cf8" />
              <h3 style={{ fontSize: '1.1rem', color: '#ffffff' }}>System Security &amp; Concurrency</h3>
            </div>
            <p style={{ fontSize: '0.85rem', color: '#94a3b8', lineHeight: 1.6, marginBottom: '20px' }}>
              Every money transfer is guaranteed atomic. Row-level pessimistic locking (<code>SELECT ... FOR UPDATE</code>) with deterministic lock ordering eliminates deadlocks and race conditions.
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <div
                style={{
                  background: 'rgba(0, 0, 0, 0.25)',
                  padding: '10px 14px',
                  borderRadius: '10px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <span style={{ fontSize: '0.825rem', color: '#94a3b8' }}>Transaction Isolation</span>
                <span className="badge badge-role">READ COMMITTED</span>
              </div>

              <div
                style={{
                  background: 'rgba(0, 0, 0, 0.25)',
                  padding: '10px 14px',
                  borderRadius: '10px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <span style={{ fontSize: '0.825rem', color: '#94a3b8' }}>Idempotency Protection</span>
                <span className="badge badge-credit">ACTIVE (SHA-256)</span>
              </div>

              <div
                style={{
                  background: 'rgba(0, 0, 0, 0.25)',
                  padding: '10px 14px',
                  borderRadius: '10px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <span style={{ fontSize: '0.825rem', color: '#94a3b8' }}>Double-Entry Ledger</span>
                <span className="badge badge-role">IMMUTABLE</span>
              </div>
            </div>
          </div>

          <div style={{ marginTop: '20px', paddingTop: '16px', borderTop: '1px solid var(--border-subtle)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Account Owner: {user?.fullName}</span>
            <span style={{ fontSize: '0.75rem', color: '#06b6d4' }}>Active JWT Session</span>
          </div>
        </div>
      </div>

      {/* Recent Transactions Section */}
      <div className="glass-panel" style={{ padding: '28px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Clock size={20} color="#06b6d4" />
            <h3 style={{ fontSize: '1.25rem', color: '#ffffff' }}>Recent Transactions</h3>
          </div>
          <button
            onClick={onNavigateToHistory}
            className="btn btn-secondary"
            style={{ padding: '6px 14px', fontSize: '0.825rem' }}
          >
            View All Transactions
            <ChevronRight size={14} />
          </button>
        </div>

        {loadingTx ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#94a3b8' }}>
            Loading ledger transactions...
          </div>
        ) : recentTransactions.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#64748b' }}>
            No transactions found. Use "Transfer Money" or "Simulate Deposit" above to start testing!
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {recentTransactions.map((tx) => {
              const isDebit = tx.entryType === 'DEBIT';
              const dateStr = new Date(tx.timestamp).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              });

              return (
                <div
                  key={tx.id}
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
                  style={{
                    background: 'rgba(15, 23, 42, 0.45)',
                    border: '1px solid var(--border-subtle)',
                    borderRadius: '12px',
                    padding: '14px 18px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    cursor: 'pointer',
                    transition: 'all 0.2s ease',
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.background = 'rgba(30, 41, 59, 0.7)';
                    e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.16)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.background = 'rgba(15, 23, 42, 0.45)';
                    e.currentTarget.style.borderColor = 'var(--border-subtle)';
                  }}
                >
                  {/* Left: Icon & Counterparty Details */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                    <div
                      style={{
                        width: '40px',
                        height: '40px',
                        borderRadius: '10px',
                        background: isDebit ? 'rgba(244, 63, 94, 0.15)' : 'rgba(16, 185, 129, 0.15)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      {isDebit ? (
                        <ArrowUpRight size={20} color="#fb7185" />
                      ) : (
                        <ArrowDownLeft size={20} color="#34d399" />
                      )}
                    </div>
                    <div>
                      <div style={{ fontWeight: 600, color: '#f8fafc', fontSize: '0.95rem' }}>
                        {isDebit ? `To: ${tx.counterpartyName}` : `From: ${tx.counterpartyName}`}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                        {dateStr} • Ref: <span style={{ fontFamily: 'monospace' }}>{tx.referenceId}</span>
                      </div>
                    </div>
                  </div>

                  {/* Right: Amount & Resulting Balance */}
                  <div style={{ textAlign: 'right' }}>
                    <div
                      style={{
                        fontSize: '1.05rem',
                        fontWeight: 700,
                        color: isDebit ? '#fb7185' : '#34d399',
                      }}
                    >
                      {isDebit ? '-' : '+'}${tx.amount?.toFixed(2)}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
                      Balance: ${tx.resultingBalance?.toFixed(2)}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Modals */}
      <TransferModal
        isOpen={isTransferOpen}
        onClose={() => setIsTransferOpen(false)}
        currentBalance={account?.balance}
        currentAccountNumber={account?.accountNumber}
        onSuccess={handleTransactionSuccess}
      />

      <DepositModal
        isOpen={isDepositOpen}
        onClose={() => setIsDepositOpen(false)}
        accountId={account?.id}
        onSuccess={handleTransactionSuccess}
      />

      <TransactionReceiptModal
        isOpen={!!activeReceipt}
        onClose={() => setActiveReceipt(null)}
        receipt={activeReceipt}
      />
    </div>
  );
};
