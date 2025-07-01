import './App.css';
import AgentChat from './AgentChat';
import CustomerPage from './CustomerPage';
import ConversationList from './ConversationList';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { useState, useRef } from 'react';

const AGENTS = [
  { id: '2cc81633-cf25-4948-90da-3e2df420c6ac', name: 'Télé-banquier 1' },
  { id: 'efe6d435-c699-45e5-897c-8bc395056084', name: 'Télé-banquier 2' }
];
const CUSTOMERS = [
  { id: 'd2707eb4-b1c6-4885-97fc-08091238699e', name: 'Customer 1', conversationId: 'f3e19418-09c8-47b0-b4d0-8577eb10c514' },
  { id: '49de2257-4301-49c4-9e4a-425648558076', name: 'Customer 2', conversationId: 'bbfd9932-da23-46a0-a37c-2d4423edc156' }
];

function App() {
  const [selectedConversationId, setSelectedConversationId] = useState(null);
  const [selectedAgent, setSelectedAgent] = useState(AGENTS[0]);
  const conversationListRef = useRef();

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={
            <div style={{ width: '100vw', height: '100vh', background: '#1f2937', display: 'flex' }}>
              <div style={{ width: 320, borderRight: '1px solid #374151', background: '#111827', height: '100vh' }}>
                <ConversationList
                  ref={conversationListRef}
                  onConversationSelect={setSelectedConversationId}
                  selectedConversationId={selectedConversationId}
                />
              </div>
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column', height: '100vh', minHeight: 0 }}>
                <div style={{ height: 64, minHeight: 64, display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px' }}>
                  <div style={{ display: 'flex', gap: 8 }}>
                    {AGENTS.map(agent => (
                      <button
                        key={agent.id}
                        onClick={() => setSelectedAgent(agent)}
                        style={{
                          padding: '8px 16px',
                          borderRadius: 8,
                          border: 'none',
                          background: selectedAgent.id === agent.id ? '#10b981' : '#374151',
                          color: '#f9fafb',
                          fontWeight: 'bold',
                          cursor: 'pointer',
                        }}
                      >
                        {agent.name}
                      </button>
                    ))}
                  </div>
                  <h2 style={{ color: '#f9fafb', margin: 0 }}>Agent View</h2>
                  <Link to="/customer" style={{ color: '#10b981', fontWeight: 'bold', textDecoration: 'none', fontSize: '1.1em' }}>
                    Go to Customer Simulation
                  </Link>
                </div>
                <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
                  {selectedConversationId ? (
                    <AgentChat conversationId={selectedConversationId} agentId={selectedAgent.id} agents={AGENTS} customers={CUSTOMERS} />
                  ) : (
                    <div style={{ color: '#9ca3af', fontSize: '1.2em', display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                      Select a conversation to start chatting
                    </div>
                  )}
                </div>
              </div>
            </div>
          }
        />
        <Route path="/customer" element={<CustomerPage agents={AGENTS} customers={CUSTOMERS} />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
