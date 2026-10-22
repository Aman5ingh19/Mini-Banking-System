import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';
import { ShieldCheck, Lock, User, ArrowRight, Sparkles } from 'lucide-react';

export const Login = ({ onSwitchToRegister }) => {
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const { showSuccess, showError } = useNotification();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await login({ usernameOrEmail, password });
      showSuccess('Welcome back to Mini Banking System', 'Authentication Succeeded');
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Invalid username or password';
      showError(msg, 'Login Failed');
    } finally {
      setLoading(false);
    }
  };

  const fillQuickDemo = (user, pass) => {
    setUsernameOrEmail(user);
    setPassword(pass);
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px',
      }}
    >
      <div
        className="glass-panel"
        style={{
          width: '100%',
          maxWidth: '440px',
          padding: '36px',
          background: 'rgba(17, 24, 39, 0.85)',
          border: '1px solid rgba(255, 255, 255, 0.12)',
          boxShadow: '0 20px 50px rgba(0, 0, 0, 0.6)',
        }}
      >
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <div
            style={{
              width: '52px',
              height: '52px',
              borderRadius: '14px',
              background: 'linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 14px',
              boxShadow: '0 8px 20px rgba(79, 70, 229, 0.4)',
            }}
          >
            <ShieldCheck size={30} color="#ffffff" />
          </div>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', marginBottom: '6px' }}>
            Mini Banking System
          </h2>
          <div style={{ fontSize: '0.875rem', color: '#94a3b8' }}>
            Simulated Banking &amp; Concurrency Engine
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Username or Email</label>
            <div style={{ position: 'relative' }}>
              <input
                type="text"
                className="form-input"
                placeholder="Enter username or email"
                value={usernameOrEmail}
                onChange={(e) => setUsernameOrEmail(e.target.value)}
                required
                style={{ paddingLeft: '40px' }}
              />
              <div
                style={{
                  position: 'absolute',
                  left: '14px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  color: '#64748b',
                }}
              >
                <User size={16} />
              </div>
            </div>
          </div>

          <div className="form-group" style={{ marginBottom: '24px' }}>
            <label className="form-label">Password</label>
            <div style={{ position: 'relative' }}>
              <input
                type="password"
                className="form-input"
                placeholder="Enter password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                style={{ paddingLeft: '40px' }}
              />
              <div
                style={{
                  position: 'absolute',
                  left: '14px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  color: '#64748b',
                }}
              >
                <Lock size={16} />
              </div>
            </div>
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', padding: '14px', fontSize: '1rem', marginBottom: '20px' }}
            disabled={loading}
          >
            {loading ? 'Authenticating...' : 'Sign In to Banking Portal'}
            <ArrowRight size={18} />
          </button>
        </form>

        {/* Quick Demo Pre-fills */}
        <div
          style={{
            background: 'rgba(0, 0, 0, 0.3)',
            padding: '12px 14px',
            borderRadius: '10px',
            border: '1px solid rgba(255, 255, 255, 0.06)',
            marginBottom: '20px',
          }}
        >
          <div
            style={{
              fontSize: '0.725rem',
              fontWeight: 700,
              color: '#818cf8',
              textTransform: 'uppercase',
              letterSpacing: '0.04em',
              marginBottom: '8px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            <Sparkles size={14} /> Quick Demo Logins (Click to Autofill)
          </div>
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            <button
              type="button"
              onClick={() => fillQuickDemo('alice_demo', 'Password123!')}
              className="btn btn-secondary"
              style={{ fontSize: '0.75rem', padding: '5px 10px', flex: 1 }}
            >
              Alice ($1,000)
            </button>
            <button
              type="button"
              onClick={() => fillQuickDemo('bob_demo', 'Password123!')}
              className="btn btn-secondary"
              style={{ fontSize: '0.75rem', padding: '5px 10px', flex: 1 }}
            >
              Bob ($1,000)
            </button>
          </div>
        </div>

        {/* Switch to Register */}
        <div style={{ textAlign: 'center', fontSize: '0.875rem', color: '#94a3b8' }}>
          Don't have a demo account?{' '}
          <button
            onClick={onSwitchToRegister}
            style={{
              background: 'transparent',
              border: 'none',
              color: '#06b6d4',
              fontWeight: 600,
              cursor: 'pointer',
              textDecoration: 'underline',
            }}
          >
            Create New Account
          </button>
        </div>
      </div>
    </div>
  );
};
