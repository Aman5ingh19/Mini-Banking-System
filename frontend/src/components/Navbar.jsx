import React from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, LogOut, Wallet, FileText, LayoutDashboard, UserCheck } from 'lucide-react';

export const Navbar = ({ currentTab, onTabChange }) => {
  const { user, account, logout } = useAuth();

  return (
    <header
      style={{
        background: 'rgba(15, 23, 42, 0.75)',
        backdropFilter: 'blur(16px)',
        WebkitBackdropFilter: 'blur(16px)',
        borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
        position: 'sticky',
        top: 0,
        zIndex: 50,
      }}
    >
      <div
        style={{
          maxWidth: '1280px',
          margin: '0 auto',
          padding: '14px 24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
        }}
      >
        {/* Left: Brand & Demo Tag */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div
              style={{
                width: '38px',
                height: '38px',
                borderRadius: '10px',
                background: 'linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 4px 12px rgba(79, 70, 229, 0.4)',
              }}
            >
              <ShieldCheck size={22} color="#ffffff" />
            </div>
            <div>
              <div style={{ fontWeight: 800, fontSize: '1.2rem', letterSpacing: '-0.02em', color: '#ffffff' }}>
                Mini Banking <span style={{ color: '#06b6d4', fontSize: '0.85rem', fontWeight: 600 }}>SYSTEM</span>
              </div>
              <div style={{ fontSize: '0.72rem', color: '#94a3b8' }}>
                Atomic Transactions Simulation
              </div>
            </div>
          </div>

          <div className="badge badge-simulated" style={{ display: 'none', md: 'inline-flex' }}>
            <span>Demo Funds Only</span>
          </div>
        </div>

        {/* Center: Navigation */}
        {user && (
          <nav style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <button
              onClick={() => onTabChange('dashboard')}
              className={`btn ${currentTab === 'dashboard' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ padding: '8px 16px', fontSize: '0.875rem' }}
            >
              <LayoutDashboard size={16} />
              Dashboard
            </button>
            <button
              onClick={() => onTabChange('transactions')}
              className={`btn ${currentTab === 'transactions' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ padding: '8px 16px', fontSize: '0.875rem' }}
            >
              <FileText size={16} />
              Transactions
            </button>
            <a
              href="http://localhost:8080/swagger-ui/index.html"
              target="_blank"
              rel="noreferrer"
              className="btn btn-secondary"
              style={{ padding: '8px 16px', fontSize: '0.875rem', textDecoration: 'none' }}
              title="OpenAPI / Swagger interactive API docs"
            >
              API Docs
            </a>
          </nav>
        )}

        {/* Right: User Profile & Logout */}
        {user ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '0.875rem', fontWeight: 700, color: '#f8fafc' }}>
                {user.fullName}
              </div>
              <div style={{ fontSize: '0.75rem', color: '#94a3b8', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '6px' }}>
                <span>@{user.username}</span>
                <span className="badge badge-role" style={{ fontSize: '0.65rem', padding: '1px 6px' }}>
                  {user.role === 'ROLE_ADMIN' ? 'ADMIN' : 'USER'}
                </span>
              </div>
            </div>

            <button
              onClick={logout}
              className="btn btn-secondary"
              style={{ padding: '8px 12px', color: '#fb7185' }}
              title="Log Out"
            >
              <LogOut size={16} />
            </button>
          </div>
        ) : (
          <div className="badge badge-simulated">
            Simulation Environment
          </div>
        )}
      </div>
    </header>
  );
};
