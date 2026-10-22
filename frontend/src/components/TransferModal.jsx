import React, { useState, useEffect } from 'react';
import { X, Send, Search, CheckCircle, AlertCircle, KeyRound } from 'lucide-react';
import { transferApi } from '../api/transferApi';
import { accountApi } from '../api/accountApi';
import { useNotification } from '../context/NotificationContext';

export const TransferModal = ({ isOpen, onClose, currentBalance, currentAccountNumber, onSuccess }) => {
  const [recipientAccount, setRecipientAccount] = useState('');
  const [recipientPreview, setRecipientPreview] = useState(null);
  const [searchingRecipient, setSearchingRecipient] = useState(false);
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState('');
  const [loading, setLoading] = useState(false);

  const { showSuccess, showError } = useNotification();

  // Generate a fresh idempotency UUID on open
  useEffect(() => {
    if (isOpen) {
      setIdempotencyKey(crypto.randomUUID());
      setRecipientAccount('');
      setRecipientPreview(null);
      setAmount('');
      setDescription('');
    }
  }, [isOpen]);

  // Debounced search for recipient account validation
  useEffect(() => {
    if (!recipientAccount || recipientAccount.trim().length < 4) {
      setRecipientPreview(null);
      return;
    }

    const timer = setTimeout(async () => {
      setSearchingRecipient(true);
      try {
        const results = await accountApi.searchRecipients(recipientAccount.trim());
        const match = results.find(
          (r) => r.accountNumber === recipientAccount.trim() || r.maskedAccountNumber === recipientAccount.trim()
        );
        if (match) {
          setRecipientPreview(match);
        } else if (results.length > 0) {
          // If typed partial query, show first matching recipient
          setRecipientPreview(results[0]);
        } else {
          setRecipientPreview(null);
        }
      } catch (err) {
        console.error('Error looking up recipient:', err);
      } finally {
        setSearchingRecipient(false);
      }
    }, 400);

    return () => clearTimeout(timer);
  }, [recipientAccount]);

  if (!isOpen) return null;

  const numAmount = parseFloat(amount) || 0;
  const isInsufficient = numAmount > (currentBalance || 0);
  const isSelfTransfer = recipientAccount.trim() === currentAccountNumber;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (numAmount <= 0) {
      showError('Amount must be greater than zero');
      return;
    }
    if (isInsufficient) {
      showError('Amount exceeds your available balance');
      return;
    }
    if (isSelfTransfer) {
      showError('You cannot transfer money to your own account');
      return;
    }

    setLoading(true);
    try {
      const receipt = await transferApi.executeTransfer(
        {
          recipientAccountNumber: recipientAccount.trim(),
          amount: numAmount,
          description: description.trim() || 'Simulated Money Transfer',
        },
        idempotencyKey
      );

      showSuccess(`Transferred $${numAmount.toFixed(2)} successfully!`, 'Transfer Completed');
      onSuccess(receipt);
      onClose();
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Transfer failed';
      showError(msg, 'Transfer Failed');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickAmount = (val) => {
    setAmount(val.toString());
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '8px',
                background: 'rgba(99, 102, 241, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Send size={18} color="#818cf8" />
            </div>
            <div>
              <h3 style={{ fontSize: '1.15rem', color: '#ffffff' }}>Send Simulated Funds</h3>
              <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                Atomic Transfer with Pessimistic Lock Protection
              </div>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              color: '#64748b',
              cursor: 'pointer',
              padding: '4px',
            }}
          >
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {/* Recipient Account Input */}
            <div className="form-group">
              <label className="form-label">Recipient Account Number</label>
              <div style={{ position: 'relative' }}>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. 4021XXXXXXXX"
                  value={recipientAccount}
                  onChange={(e) => setRecipientAccount(e.target.value)}
                  required
                />
                <div
                  style={{
                    position: 'absolute',
                    right: '12px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    color: '#64748b',
                  }}
                >
                  <Search size={16} />
                </div>
              </div>

              {/* Verified Recipient Box */}
              {recipientPreview && (
                <div
                  style={{
                    marginTop: '8px',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    background: 'rgba(16, 185, 129, 0.1)',
                    border: '1px solid rgba(16, 185, 129, 0.25)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    fontSize: '0.825rem',
                    color: '#34d399',
                  }}
                >
                  <CheckCircle size={15} />
                  <span>
                    Verified: <strong>{recipientPreview.recipientName}</strong> ({recipientPreview.maskedAccountNumber})
                  </span>
                </div>
              )}
            </div>

            {/* Amount Input */}
            <div className="form-group">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <label className="form-label">Transfer Amount (USD)</label>
                <span style={{ fontSize: '0.75rem', color: isInsufficient ? '#f43f5e' : '#94a3b8' }}>
                  Available: ${currentBalance?.toFixed(2)}
                </span>
              </div>
              <input
                type="number"
                step="0.01"
                min="0.01"
                max={currentBalance}
                className="form-input"
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
              />

              {/* Quick Select Buttons */}
              <div style={{ display: 'flex', gap: '6px', marginTop: '8px' }}>
                {[25, 50, 100, 250].map((val) => (
                  <button
                    key={val}
                    type="button"
                    onClick={() => handleQuickAmount(val)}
                    className="btn btn-secondary"
                    style={{ padding: '4px 10px', fontSize: '0.75rem', borderRadius: '6px' }}
                  >
                    +${val}
                  </button>
                ))}
                <button
                  type="button"
                  onClick={() => handleQuickAmount(currentBalance || 0)}
                  className="btn btn-secondary"
                  style={{ padding: '4px 10px', fontSize: '0.75rem', borderRadius: '6px', color: '#06b6d4' }}
                >
                  MAX
                </button>
              </div>

              {isInsufficient && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#fb7185', fontSize: '0.8rem', marginTop: '6px' }}>
                  <AlertCircle size={14} />
                  <span>Amount exceeds available balance</span>
                </div>
              )}
            </div>

            {/* Note / Description */}
            <div className="form-group">
              <label className="form-label">Memo / Description (Optional)</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. Split dinner, Project payment"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                maxLength={100}
              />
            </div>

            {/* Idempotency Key Info Pill */}
            <div
              style={{
                padding: '10px 14px',
                borderRadius: '8px',
                background: 'rgba(0, 0, 0, 0.3)',
                border: '1px solid rgba(255, 255, 255, 0.06)',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontSize: '0.75rem',
                color: '#64748b',
              }}
            >
              <KeyRound size={15} color="#818cf8" />
              <div style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                Idempotency Key: <code style={{ color: '#94a3b8' }}>{idempotencyKey.substring(0, 18)}...</code>
              </div>
              <span className="badge badge-role" style={{ fontSize: '0.6rem' }}>PROTECTED</span>
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" onClick={onClose} className="btn btn-secondary" disabled={loading}>
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading || numAmount <= 0 || isInsufficient || !recipientAccount}
            >
              <Send size={16} />
              {loading ? 'Processing Transaction...' : `Confirm $${numAmount.toFixed(2)}`}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
