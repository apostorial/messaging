import './App.css';
import AgentChat from './AgentChat';
import CustomerChat from './CustomerChat';
import ConversationList from './ConversationList';
import { useState, useRef } from 'react';

function App() {
  const [selectedConversationId, setSelectedConversationId] = useState(null);
  const conversationListRef = useRef();

  const handleConversationSelect = (conversationId) => {
    setSelectedConversationId(conversationId);
    setTimeout(() => {
      if (conversationListRef.current?.fetchConversations) {
        conversationListRef.current.fetchConversations();
      }
    }, 100);
  };

  return (
    <div style={{ display: 'flex', height: '100vh' }}>
      <ConversationList 
        ref={conversationListRef}
        onConversationSelect={handleConversationSelect}
        selectedConversationId={selectedConversationId}
      />
      <div style={{ display: 'flex', flex: 1, gap: '20px', padding: '20px' }}>
        <div>
          <h2>Agent View</h2>
          <AgentChat conversationId={selectedConversationId} />
        </div>
        <div>
          <h2>Customer View</h2>
          <CustomerChat conversationId={selectedConversationId} />
        </div>
      </div>
    </div>
  );
}

export default App;
