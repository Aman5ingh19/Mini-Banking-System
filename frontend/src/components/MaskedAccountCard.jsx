import React, { useState } from 'react';
import { Eye, EyeOff, Copy, Check, Send, PlusCircle, Shield, RefreshCw } from 'lucide-react';
import { useNotification } from '../context/NotificationContext';

export const MaskedAccountCard = ({ account, onOpenTransfer, onOpenDeposit, onRefresh }) => {
  const [showFullNumber, setShowFullNumber] = useState(false);
  const [copied, setCopied] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const { showSuccess } = useNotification();

  if (!account) return null;

  const handleCopy = () => {
    navigator.clipboard.writeText(account.accountNumber);
    setCopied(true);
    showSuccess('Account number copied to clipboard', 'Copied');
    setTimeout(() => setCopied(false), 2000);
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    await onRefresh();
    setTimeout(() => setRefreshing(false), 500);
  };

  const formattedBalance = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: account.currency || 'USD',
    minimumFractionDigits: 2,
  }).format(account.balance || 0);

  const displayAccountNumber = showFullNumber
    ? account.accountNumber.replace(/(\d{4})/g, '$1 ').trim()
    : account.maskedAccountNumber;

  return (
    <div
      className="glass-panel"
      style={{
        padding: '30px',
        position: 'relative',
        overflow: 'hidden',
        background: 'linear-gradient(135deg, rgba(30, 41, 59, 0.8) 0%, rgba(15, 23, 42, 0.9) 100%)',
        border: '1px solid rgba(255, 255, 255, 0.12)',
      }}
    >
      {/* Decorative background glow circles */}
      <div
        style={{
          position: 'absolute',
          top: '-60px',
          right: '-60px',
          width: '200px',
          height: '200px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(99, 102, 241, 0.25) 0%, transparent 70%)',
          pointerEvents: 'none',
        }}
      />
      <div
        style={{
          position: 'absolute',
          bottom: '-40px',
          left: '-40px',
          width: '160px',
          height: '160px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(6, 182, 212, 0.2) 0%, transparent 70%)',
          pointerEvents: 'none',
        }}
      />

      {/* Top row: Label, Currency badge, Refresh button */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Shield size={18} color="#06b6d4" />
          <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Primary Checking Account
          </span>
          <span className="badge badge-credit" style={{ fontSize: '0.65rem' }}>
            {account.status || 'ACTIVE'}
          </span>
        </div>

        <button
          onClick={handleRefresh}
          className="btn btn-secondary"
          style={{ padding: '6px 10px', fontSize: '0.75rem', borderRadius: '8px' }}
          title="Refresh Balance"
          disabled={refreshing}
        >
          <RefreshCw size={14} className={refreshing ? 'pulse' : ''} />
          Sync
        </button>
      </div>

      {/* Balance Display */}
      <div style={{ marginBottom: '28px' }}>
        <div style={{ fontSize: '0.85rem', color: '#94a3b8', marginBottom: '4px' }}>
          Available Balance (Simulated Funds)
        </div>
        <div
          style={{
            fontSize: '2.8rem',
            fontWeight: 800,
            letterSpacing: '-0.03em',
            color: '#ffffff',
            display: 'flex',
            alignItems: 'baseline',
            gap: '8px',
          }}
        >
          {formattedBalance}
          <span style={{ fontSize: '1rem', fontWeight: 600, color: '#06b6d4' }}>
            {account.currency || 'USD'}
          </span>
        </div>
      </div>

      {/* Account Number with Mask Toggle and Copy */}
      <div
        style={{
          background: 'rgba(0, 0, 0, 0.35)',
          padding: '12px 18px',
          borderRadius: '12px',
          border: '1px solid rgba(255, 255, 255, 0.06)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '26px',
        }}
      >
        <div>
          <div style={{ fontSize: '0.7rem', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '2px' }}>
            Account Number
          </div>
          <div style={{ fontFamily: 'monospace', fontSize: '1.05rem', fontWeight: 600, color: '#f1f5f9', letterSpacing: '0.12em' }}>
            {displayAccountNumber}
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button
            onClick={() => setShowFullNumber(!showFullNumber)}
            className="btn btn-secondary"
            style={{ padding: '6px 10px', borderRadius: '8px' }}
            title={showFullNumber ? 'Hide Account Number' : 'Reveal Full Account Number'}
          >
            {showFullNumber ? <EyeOff size={15} /> : <Eye size={15} />}
          </button>
          <button
            onClick={handleCopy}
            className="btn btn-secondary"
            style={{ padding: '6px 10px', borderRadius: '8px' }}
            title="Copy Account Number"
          >
            {copied ? <Check size={15} color="#10b981" /> : <Copy size={15} />}
          </button>
        </div>
      </div>

      {/* Action Buttons */}
      <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
        <button
          onClick={onOpenTransfer}
          className="btn btn-primary"
          style={{ flex: 1, minWidth: '150px', padding: '12px 20px' }}
        >
          <Send size={18} />
          Transfer Money
        </button>
        <button
          onClick={onOpenDeposit}
          className="btn btn-secondary"
          style={{ flex: 1, minWidth: '150px', padding: '12px 20px' }}
        >
          <PlusCircle size={18} color="#10b981" />
          Simulate Deposit
        </button>
      </div>
    </div>
  );
};
