import React, { createContext, useContext, useState, useCallback } from 'react';
import { CheckCircle, AlertTriangle, XCircle, Info, X } from 'lucide-react';

const NotificationContext = createContext(null);

export const NotificationProvider = ({ children }) => {
  const [notifications, setNotifications] = useState([]);

  const addNotification = useCallback((type, message, title = '') => {
    const id = Date.now() + Math.random().toString();
    setNotifications((prev) => [...prev, { id, type, message, title }]);

    setTimeout(() => {
      setNotifications((prev) => prev.filter((n) => n.id !== id));
    }, 4500);
  }, []);

  const removeNotification = useCallback((id) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  }, []);

  const showSuccess = useCallback((msg, title = 'Success') => addNotification('success', msg, title), [addNotification]);
  const showError = useCallback((msg, title = 'Error') => addNotification('danger', msg, title), [addNotification]);
  const showWarning = useCallback((msg, title = 'Warning') => addNotification('warning', msg, title), [addNotification]);
  const showInfo = useCallback((msg, title = 'Information') => addNotification('info', msg, title), [addNotification]);

  return (
    <NotificationContext.Provider
      value={{ showSuccess, showError, showWarning, showInfo }}
    >
      {children}
      {/* Toast Container */}
      <div
        style={{
          position: 'fixed',
          top: '20px',
          right: '20px',
          zIndex: 9999,
          display: 'flex',
          flexDirection: 'column',
          gap: '10px',
          maxWidth: '380px',
          width: 'calc(100% - 40px)',
          pointerEvents: 'none',
        }}
      >
        {notifications.map((n) => {
          let borderColor = 'rgba(99, 102, 241, 0.4)';
          let iconColor = '#818cf8';
          let IconComponent = Info;

          if (n.type === 'success') {
            borderColor = 'rgba(16, 185, 129, 0.4)';
            iconColor = '#10b981';
            IconComponent = CheckCircle;
          } else if (n.type === 'danger') {
            borderColor = 'rgba(244, 63, 94, 0.4)';
            iconColor = '#f43f5e';
            IconComponent = XCircle;
          } else if (n.type === 'warning') {
            borderColor = 'rgba(245, 158, 11, 0.4)';
            iconColor = '#f59e0b';
            IconComponent = AlertTriangle;
          }

          return (
            <div
              key={n.id}
              style={{
                pointerEvents: 'auto',
                background: '#111827',
                border: `1px solid ${borderColor}`,
                borderRadius: '12px',
                padding: '14px 16px',
                display: 'flex',
                alignItems: 'flex-start',
                gap: '12px',
                boxShadow: '0 10px 25px rgba(0, 0, 0, 0.6)',
                animation: 'fadeIn 0.2s ease-out',
              }}
            >
              <IconComponent size={20} color={iconColor} style={{ marginTop: '2px', flexShrink: 0 }} />
              <div style={{ flex: 1 }}>
                {n.title && (
                  <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#f8fafc', marginBottom: '2px' }}>
                    {n.title}
                  </div>
                )}
                <div style={{ fontSize: '0.85rem', color: '#94a3b8', lineHeight: 1.4 }}>
                  {n.message}
                </div>
              </div>
              <button
                onClick={() => removeNotification(n.id)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  color: '#64748b',
                  cursor: 'pointer',
                  padding: '2px',
                }}
              >
                <X size={16} />
              </button>
            </div>
          );
        })}
      </div>
    </NotificationContext.Provider>
  );
};

export const useNotification = () => useContext(NotificationContext);
