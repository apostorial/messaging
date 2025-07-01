import CustomerChat from './CustomerChat';
import { Link } from 'react-router-dom';
import { useState } from 'react';

function CustomerPage({ agents, customers }) {
  const [selectedCustomer, setSelectedCustomer] = useState(customers[0]);
  return (
    <div
      style={{
        width: '100vw',
        height: '100vh',
        background: '#111827',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        boxSizing: 'border-box',
        flexDirection: 'column',
      }}
    >
      <Link to="/" style={{ color: '#10b981', fontWeight: 'bold', textDecoration: 'none', marginBottom: 24, fontSize: '1.1em' }}>
        ← Back to Agent View
      </Link>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        {customers.map(customer => (
          <button
            key={customer.id}
            onClick={() => setSelectedCustomer(customer)}
            style={{
              padding: '8px 16px',
              borderRadius: 8,
              border: 'none',
              background: selectedCustomer.id === customer.id ? '#10b981' : '#374151',
              color: '#f9fafb',
              fontWeight: 'bold',
              cursor: 'pointer',
            }}
          >
            {customer.name}
          </button>
        ))}
      </div>
      <div style={{
        width: 360,
        height: 700,
        border: '12px solid #374151',
        borderRadius: 36,
        boxShadow: '0 8px 32px rgba(0,0,0,0.35)',
        background: '#1f2937',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}>
        <div style={{
          background: '#111827',
          color: '#f9fafb',
          textAlign: 'center',
          padding: '12px 0',
          fontWeight: 'bold',
          fontSize: '1.2em',
          borderBottom: '1px solid #374151',
        }}>
          Customer Chat (Phone Simulation)
        </div>
        <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', height: '100%' }}>
          <CustomerChat conversationId={selectedCustomer.conversationId} customerId={selectedCustomer.id} agents={agents} customers={customers} />
        </div>
      </div>
    </div>
  );
}

export default CustomerPage; 