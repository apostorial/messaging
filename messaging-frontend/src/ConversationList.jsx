import React, { useState, useEffect, forwardRef, useImperativeHandle } from 'react';
import './App.css';

const ConversationList = forwardRef(({ onConversationSelect, selectedConversationId }, ref) => {
  const [conversations, setConversations] = useState([]);

  const fetchConversations = () => {
    fetch('http://localhost:8080/conversations')
      .then(response => response.json())
      .then(data => setConversations(data))
      .catch(error => console.error('Error fetching conversations:', error));
  };

  useImperativeHandle(ref, () => ({
    fetchConversations
  }));

  useEffect(() => {
    fetchConversations();
    
    // Refresh conversations every 5 seconds to keep order updated
    const interval = setInterval(fetchConversations, 5000);
    
    return () => clearInterval(interval);
  }, []);

  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    const now = new Date();
    const diffInHours = (now - date) / (1000 * 60 * 60);
    
    if (diffInHours < 24) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else if (diffInHours < 48) {
      return 'Yesterday';
    } else {
      return date.toLocaleDateString();
    }
  };

  return (
    <div className="conversation-list">
      <h3>Conversations</h3>
      {Array.isArray(conversations) && conversations.length === 0 ? (
        <div style={{ padding: '20px', textAlign: 'center', color: '#9ca3af' }}>
          No conversations yet
        </div>
      ) : (
        Array.isArray(conversations) && conversations.map((conv) => (
          <div
            key={conv.id}
            className={`conversation-item ${selectedConversationId === conv.id ? 'selected' : ''}`}
            onClick={() => onConversationSelect(conv.id)}
          >
            <div className="conversation-header">
              <span className="customer-name">{conv.customerName}</span>
              {conv.unreadCount > 0 && (
                <span className="unread-badge">{conv.unreadCount}</span>
              )}
              <span className="last-message-time">{formatTime(conv.lastMessageTime)}</span>
            </div>
            <div className="last-message-preview">
              {conv.lastMessageContent || 'No messages yet'}
            </div>
          </div>
        ))
      )}
    </div>
  );
});

export default ConversationList; 