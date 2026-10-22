import React, { useState } from 'react';
import { X, PlusCircle, AlertCircle } from 'lucide-react';
import { adminApi } from '../api/adminApi';
import { useNotification } from '../context/NotificationContext';

export const DepositModal = ({ isOpen, onClose, accountId, onSuccess }) => {
  const [amount, setAmount] = useState('250.00');
  const [description, setDescription] = useState('Simulated Demo Deposit');
  const [loading, setLoading] = useState(false);

  const { showSuccess, showError } = useNotification();

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    const numAmount = parseFloat(amount);
    if (!numAmount || numAmount <= 0) {
      showError('Deposit amount must be positive');
      return;
    }

    setLoading(true);
    try {
      const receipt = await adminApi.deposit(accountId, {
        amount: numAmount,
        description: description.trim() || 'Simulated Test Credit',
      });
      showSuccess(`Successfully credited $${numAmount.toFixed(2)} to account!`, 'Deposit Succeeded');
      onSuccess(receipt);
      onClose();
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Deposit simulation failed';
      showError(msg, 'Deposit Failed');
    } finally {
      setLoading(false);
    }
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
                background: 'rgba(16, 185, 129, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <PlusCircle size={18} color="#10b981" />
            </div>
            <div>
              <h3 style={{ fontSize: '1.15rem', color: '#ffffff' }}>Add Simulated Funds</h3>
              <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                Credit demo funds for testing transfers
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
            <div className="form-group">
              <label className="form-label">Deposit Amount (USD)</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                className="form-input"
                placeholder="250.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
              />

              <div style={{ display: 'flex', gap: '6px', marginTop: '8px' }}>
                {[100, 250, 500, 1000].map((val) => (
                  <button
                    key={val}
                    type="button"
                    onClick={() => setAmount(val.toString())}
                    className="btn btn-secondary"
                    style={{ padding: '4px 10px', fontSize: '0.75rem', borderRadius: '6px' }}
                  >
                    +${val}
                  </button>
                ))}
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Description Note</label>
              <input
                type="text"
                className="form-input"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                maxLength={100}
              />
            </div>

            <div
              style={{
                padding: '10px 14px',
                borderRadius: '8px',
                background: 'rgba(245, 158, 11, 0.1)',
                border: '1px solid rgba(245, 158, 11, 0.25)',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontSize: '0.75rem',
                color: '#fbbf24',
              }}
            >
              <AlertCircle size={15} style={{ flexShrink: 0 }} />
              <span>Simulated demo funds only. Real money is never used.</span>
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" onClick={onClose} className="btn btn-secondary" disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn btn-success" disabled={loading || !amount || parseFloat(amount) <= 0}>
              <PlusCircle size={16} />
              {loading ? 'Crediting...' : `Deposit $${parseFloat(amount || 0).toFixed(2)}`}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
