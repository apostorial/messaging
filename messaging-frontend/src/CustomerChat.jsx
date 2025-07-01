import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import './App.css';
import React from 'react';
import { Reply, Pencil, Paperclip } from 'lucide-react';

function CustomerChat({ conversationId, customerId, agents = [], customers = [] }) {
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [replyToMessage, setReplyToMessage] = useState(null);
  const [highlightedMsgId, setHighlightedMsgId] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const stompClient = useRef(null);
  const messageRefs = useRef({});
  const fileInputRef = useRef();
  const [editingMsgId, setEditingMsgId] = useState(null);
  const [previewImage, setPreviewImage] = useState(null);

  const senderId = customerId || import.meta.env.VITE_CUSTOMER_ID;

  // Helper to get sender name
  const getSenderName = (id) => {
    const agent = agents.find(a => a.id === id);
    if (agent) return agent.name;
    const customer = customers.find(c => c.id === id);
    if (customer) return customer.name;
    return 'Unknown';
  };

  useEffect(() => {
    if (!conversationId) return;
    
    fetch(`http://localhost:8080/conversations/${conversationId}/messages`)
      .then(response => response.json())
      .then(data => setMessages(data))
      .catch(error => console.error('Error fetching messages:', error));

    // Mark agent messages as read when customer opens the conversation
    fetch(`http://localhost:8080/conversations/${conversationId}/read?readerType=CUSTOMER`, { method: 'PUT' });

    const socketFactory = () => new SockJS('http://localhost:8080/ws-sockjs');
    const client = new Client({
      webSocketFactory: socketFactory,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/conversation/${conversationId}`, (message) => {
          const receivedMessage = JSON.parse(message.body);
          setMessages(prevMessages => {
            const idx = prevMessages.findIndex(m => m.id === receivedMessage.id);
            if (idx !== -1) {
              const updated = [...prevMessages];
              updated[idx] = receivedMessage;
              return updated;
            }
            return [...prevMessages, receivedMessage];
          });
        });
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      },
    });
    client.activate();
    stompClient.current = client;
    return () => {
      if (stompClient.current) {
        stompClient.current.deactivate();
      }
    };
  }, [conversationId]);

  const sendMessage = async () => {
    if (editingMsgId) {
      // Edit message
      await fetch(`http://localhost:8080/messages/${editingMsgId}/edit?content=${encodeURIComponent(inputMessage)}`, {
        method: 'PUT',
      });
      setEditingMsgId(null);
      setInputMessage('');
      setReplyToMessage(null);
      return;
    }
    if (selectedFile) {
      // Upload file
      const formData = new FormData();
      formData.append('file', selectedFile);
      formData.append('senderId', senderId);
      formData.append('conversationId', conversationId);
      if (replyToMessage?.id) formData.append('replyTo', replyToMessage.id);
      if (inputMessage.trim()) formData.append('content', inputMessage.trim());
      await fetch('http://localhost:8080/upload', {
        method: 'POST',
        body: formData,
      });
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      setInputMessage('');
      setReplyToMessage(null);
      // Remove from AgentChat preview
      window.localStorage.removeItem('customerSelectedFile');
      window.dispatchEvent(new Event('customerSelectedFileChanged'));
      return;
    }
    if (inputMessage.trim() && stompClient.current?.connected) {
      const chatMessage = {
        content: inputMessage,
        senderId: senderId,
        conversationId: conversationId,
        replyTo: replyToMessage?.id,
      };
      stompClient.current.publish({
        destination: '/app/chat.sendMessage',
        body: JSON.stringify(chatMessage),
      });
      setInputMessage('');
      setReplyToMessage(null);
    }
  };

  const handleReplyClick = (message) => {
    setReplyToMessage(message);
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setSelectedFile(file);
    // Broadcast file info to AgentChat
    window.localStorage.setItem('customerSelectedFile', JSON.stringify({
      name: file.name,
      type: file.type,
      size: file.size,
      lastModified: file.lastModified
    }));
    window.dispatchEvent(new Event('customerSelectedFileChanged'));
  };

  const handleRemoveFile = () => {
    setSelectedFile(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
    // Remove from AgentChat preview
    window.localStorage.removeItem('customerSelectedFile');
    window.dispatchEvent(new Event('customerSelectedFileChanged'));
  };

  if (!conversationId) {
    return (
      <div className="chat-container">
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%', color: '#9ca3af' }}>
          Select a conversation to start chatting
        </div>
      </div>
    );
  }

  return (
    <div className="chat-container">
      <div className="messages-area">
        {messages.map((msg, index) => {
          const showSenderName =
            index === 0 || messages[index - 1].senderId !== msg.senderId;
          const isSent = msg.senderId === senderId;
          // Find the replied-to message, if any
          const repliedMsg = msg.replyTo ? messages.find(m => m.id === msg.replyTo) : null;
          return (
            <div
              key={index}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: isSent ? 'flex-end' : 'flex-start',
                marginBottom: 4,
              }}
            >
              {showSenderName && (
                <div style={{ color: '#10b981', fontWeight: 'bold', marginBottom: 2, fontSize: '0.95em' }}>
                  {getSenderName(msg.senderId)}
                </div>
              )}
              <div
                className={`message ${isSent ? 'sent' : 'received'}${highlightedMsgId === msg.id ? ' highlight' : ''}`}
                ref={msg.id ? messageRefs.current[msg.id] : null}
              >
                {/* Show replied-to message snippet if exists */}
                {repliedMsg && (
                  <div
                    className="reply-snippet"
                    onClick={e => {
                      e.stopPropagation();
                      if (repliedMsg.id && messageRefs.current[repliedMsg.id]) {
                        messageRefs.current[repliedMsg.id].current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        setHighlightedMsgId(repliedMsg.id);
                        setTimeout(() => setHighlightedMsgId(null), 1000);
                      }
                    }}
                    style={{ cursor: 'pointer', background: '#222c3a', borderRadius: 6, padding: 4, marginBottom: 6 }}
                  >
                    <strong>Replying to:</strong>
                    <div className="reply-content" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      {repliedMsg.fileUrl ? (
                        repliedMsg.messageType === 'IMAGE' ? (
                          <img src={repliedMsg.fileUrl} alt="thumb" style={{ width: 32, height: 32, objectFit: 'cover', borderRadius: 4, marginRight: 4 }} />
                        ) : (
                          <a href={repliedMsg.fileUrl} target="_blank" rel="noopener noreferrer" style={{ color: '#10b981', fontSize: 18, marginRight: 4 }} title="Download attachment">📎</a>
                        )
                      ) : (
                        repliedMsg.content
                      )}
                    </div>
                  </div>
                )}
                {/* Show file if present */}
                {msg.fileUrl && (
                  msg.messageType === 'IMAGE' ? (
                    <img 
                      src={msg.fileUrl} 
                      alt="uploaded" 
                      style={{ maxWidth: '200px', borderRadius: '10px', marginBottom: '6px', cursor: 'pointer' }} 
                      onClick={() => setPreviewImage(msg.fileUrl)}
                    />
                  ) : (
                    <a href={msg.fileUrl} target="_blank" rel="noopener noreferrer" style={{ color: '#10b981' }}>
                      Download file
                    </a>
                  )
                )}
                <p>{msg.content}
                  {msg.edited && (
                    <span style={{ color: '#9ca3af', fontSize: '0.8em', marginLeft: 6 }}>(edited)</span>
                  )}
                </p>
                <span style={{ fontSize: '0.75em', color: '#9ca3af', display: 'block', textAlign: 'right', marginTop: 5 }}>
                  {new Date(msg.timestamp).toLocaleTimeString()}
                </span>
                {/* Reply icon for setting reply target and edit icon for editing own messages */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
                  <span
                    style={{ cursor: 'pointer', color: '#10b981', fontSize: 18 }}
                    title="Reply to this message"
                    onClick={e => {
                      e.stopPropagation();
                      handleReplyClick(msg);
                    }}
                  >
                    <Reply size={16} />
                  </span>
                  {msg.senderId === senderId && (
                    <span
                      style={{ cursor: 'pointer', color: '#f59e42', fontSize: 18 }}
                      title="Edit this message"
                      onClick={e => {
                        e.stopPropagation();
                        setInputMessage(msg.content);
                        setEditingMsgId(msg.id);
                        setReplyToMessage(null);
                        setTimeout(() => {
                          document.getElementById('chat-input')?.focus();
                        }, 50);
                      }}
                    >
                      <Pencil size={16} />
                    </span>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
      <div className="input-area">
        {replyToMessage && (
          <div className="reply-preview">
            <p>Replying to: {replyToMessage.content}</p>
          </div>
        )}
        {selectedFile && (
          <div className="file-preview" style={{ display: 'flex', alignItems: 'center', marginBottom: 6 }}>
            {selectedFile.type.startsWith('image') ? (
              <img src={URL.createObjectURL(selectedFile)} alt="preview" style={{ maxWidth: 60, maxHeight: 60, borderRadius: 8, marginRight: 8 }} />
            ) : null}
            <span style={{ color: '#10b981', marginRight: 8, maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'inline-block' }}>{selectedFile.name}</span>
            <button onClick={handleRemoveFile} style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontSize: 18 }} title="Remove file">✕</button>
          </div>
        )}
        <input
          type="file"
          style={{ display: 'none' }}
          id="file-upload"
          onChange={handleFileChange}
          ref={fileInputRef}
        />
        <button
          type="button"
          onClick={() => document.getElementById('file-upload').click()}
          style={{ 
            background: 'none', 
            border: 'none', 
            cursor: 'pointer', 
            padding: '8px', 
            marginRight: '8px',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
          title="Upload file"
        >
          <Paperclip size={20} color="#10b981" />
        </button>
        <input
          type="text"
          value={inputMessage}
          onChange={(e) => setInputMessage(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
          placeholder="Type a message..."
          style={editingMsgId ? { border: '2px solid #10b981' } : {}}
          id="chat-input"
        />
        <button onClick={sendMessage}>Send</button>
      </div>
      {/* Image Preview Modal */}
      {previewImage && (
        <div 
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            zIndex: 1000,
            cursor: 'pointer'
          }}
          onClick={() => setPreviewImage(null)}
        >
          <img 
            src={previewImage} 
            alt="preview" 
            style={{ 
              maxWidth: '90%', 
              maxHeight: '90%', 
              objectFit: 'contain',
              borderRadius: '8px'
            }}
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      )}
    </div>
  );
}

export default CustomerChat; 