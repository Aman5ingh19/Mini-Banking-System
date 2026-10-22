import React, { useState } from 'react';
import { X, CheckCircle, Copy, Check, Printer, ShieldCheck } from 'lucide-react';
import { useNotification } from '../context/NotificationContext';

export const TransactionReceiptModal = ({ isOpen, onClose, receipt }) => {
  const [copied, setCopied] = useState(false);
  const { showSuccess } = useNotification();

  if (!isOpen || !receipt) return null;

  const handleCopyRef = () => {
    navigator.clipboard.writeText(receipt.referenceId);
    setCopied(true);
    showSuccess('Reference ID copied to clipboard', 'Copied');
    setTimeout(() => setCopied(false), 2000);
  };

  const handlePrint = () => {
    window.print();
  };

  const formattedAmount = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: receipt.currency || 'USD',
  }).format(receipt.amount || 0);

  const formattedDate = receipt.timestamp
    ? new Date(receipt.timestamp).toLocaleString('en-US', {
        dateStyle: 'medium',
        timeStyle: 'medium',
      })
    : new Date().toLocaleString();

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '480px' }}>
        {/* Receipt Header */}
        <div
          style={{
            background: 'linear-gradient(135deg, rgba(79, 70, 229, 0.2) 0%, rgba(6, 182, 212, 0.2) 100%)',
            padding: '24px',
            textAlign: 'center',
            position: 'relative',
            borderBottom: '1px solid var(--border-subtle)',
          }}
        >
          <button
            onClick={onClose}
            style={{
              position: 'absolute',
              right: '16px',
              top: '16px',
              background: 'transparent',
              border: 'none',
              color: '#94a3b8',
              cursor: 'pointer',
            }}
          >
            <X size={20} />
          </button>

          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '50%',
              background: 'rgba(16, 185, 129, 0.2)',
              border: '2px solid #10b981',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 12px',
            }}
          >
            <CheckCircle size={26} color="#10b981" />
          </div>

          <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#34d399', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Transaction Receipt
          </div>
          <div style={{ fontSize: '2.2rem', fontWeight: 800, color: '#ffffff', margin: '4px 0' }}>
            {formattedAmount}
          </div>
          <div className="badge badge-credit" style={{ fontSize: '0.7rem' }}>
            {receipt.status || 'SUCCESS'}
          </div>
        </div>

        {/* Receipt Body */}
        <div className="modal-body" style={{ padding: '20px 24px' }}>
          {/* Reference ID Pill */}
          <div
            style={{
              background: 'rgba(0, 0, 0, 0.3)',
              padding: '10px 14px',
              borderRadius: '10px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '18px',
              border: '1px solid rgba(255, 255, 255, 0.05)',
            }}
          >
            <div>
              <div style={{ fontSize: '0.68rem', color: '#64748b', textTransform: 'uppercase' }}>
                Transaction Reference ID
              </div>
              <div style={{ fontFamily: 'monospace', fontSize: '0.925rem', fontWeight: 700, color: '#06b6d4' }}>
                {receipt.referenceId}
              </div>
            </div>
            <button
              onClick={handleCopyRef}
              className="btn btn-secondary"
              style={{ padding: '6px 8px', borderRadius: '6px' }}
              title="Copy Reference ID"
            >
              {copied ? <Check size={14} color="#10b981" /> : <Copy size={14} />}
            </button>
          </div>

          {/* Key-Value Breakdown */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
              <span style={{ color: '#94a3b8' }}>Timestamp</span>
              <span style={{ color: '#f8fafc', fontWeight: 500 }}>{formattedDate}</span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
              <span style={{ color: '#94a3b8' }}>Transfer Type</span>
              <span style={{ color: '#f8fafc', fontWeight: 600 }}>{receipt.type || 'TRANSFER'}</span>
            </div>

            <div style={{ height: '1px', background: 'var(--border-subtle)', margin: '4px 0' }} />

            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
              <span style={{ color: '#94a3b8' }}>From (Sender)</span>
              <div style={{ textAlign: 'right' }}>
                <div style={{ color: '#f8fafc', fontWeight: 600 }}>{receipt.senderName}</div>
                <div style={{ color: '#64748b', fontSize: '0.75rem', fontFamily: 'monospace' }}>
                  {receipt.sourceMaskedAccountNumber}
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
              <span style={{ color: '#94a3b8' }}>To (Recipient)</span>
              <div style={{ textAlign: 'right' }}>
                <div style={{ color: '#f8fafc', fontWeight: 600 }}>{receipt.recipientName}</div>
                <div style={{ color: '#64748b', fontSize: '0.75rem', fontFamily: 'monospace' }}>
                  {receipt.destinationMaskedAccountNumber}
                </div>
              </div>
            </div>

            {receipt.description && (
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
                <span style={{ color: '#94a3b8' }}>Memo</span>
                <span style={{ color: '#f8fafc', fontStyle: 'italic' }}>{receipt.description}</span>
              </div>
            )}

            {receipt.resultingBalance !== undefined && (
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
                <span style={{ color: '#94a3b8' }}>New Available Balance</span>
                <span style={{ color: '#34d399', fontWeight: 700 }}>
                  ${receipt.resultingBalance?.toFixed(2)}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Footer Actions */}
        <div className="modal-footer" style={{ justifyContent: 'space-between' }}>
          <button onClick={handlePrint} className="btn btn-secondary" style={{ padding: '8px 14px' }}>
            <Printer size={16} />
            Print Receipt
          </button>
          <button onClick={onClose} className="btn btn-primary" style={{ padding: '8px 20px' }}>
            Done
          </button>
        </div>
      </div>
    </div>
  );
};
