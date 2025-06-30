import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import './App.css';
import React from 'react';
import { Reply, Pencil, Paperclip } from 'lucide-react';

function CustomerChat() {
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

  const conversationId = import.meta.env.VITE_CONVERSATION_ID;
  const senderId = import.meta.env.VITE_CUSTOMER_ID;

  useEffect(() => {
    fetch(`http://localhost:8080/conversations/${conversationId}/messages`)
      .then(response => response.json())
      .then(data => setMessages(data))
      .catch(error => console.error('Error fetching messages:', error));

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
              // Replace the old message
              const updated = [...prevMessages];
              updated[idx] = receivedMessage;
              return updated;
            }
            // Otherwise, append as new
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

  return (
    <div className="chat-container">
      <div className="messages-area">
      {messages.map((msg, index) => {
          // Find the replied-to message, if any
          const repliedMsg = msg.replyTo
            ? messages.find(m => m.id === msg.replyTo)
            : null;

          // Attach ref to each message by id
          if (msg.id && !messageRefs.current[msg.id]) {
            messageRefs.current[msg.id] = React.createRef();
          }

          const handleReplySnippetClick = (e) => {
            e.stopPropagation();
            if (repliedMsg && repliedMsg.id && messageRefs.current[repliedMsg.id]) {
              messageRefs.current[repliedMsg.id].current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
              setHighlightedMsgId(repliedMsg.id);
              setTimeout(() => setHighlightedMsgId(null), 1000);
            }
          };

          return (
            <div
              key={index}
              className={`message ${msg.senderId === senderId ? 'sent' : 'received'}`}
              style={{
                boxShadow: highlightedMsgId === msg.id ? '0 0 0 3px #facc15' : 'none',
                transition: 'box-shadow 0.5s'
              }}
              ref={msg.id ? messageRefs.current[msg.id] : null}
            >
              {/* Show replied-to message snippet if exists */}
              {repliedMsg && (
                <div className="reply-snippet" onClick={handleReplySnippetClick} style={{ cursor: 'pointer' }}>
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
              {msg.fileUrl ? (
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
              ) : null}
              <p>{msg.content}
                {msg.edited && (
                  <span style={{ fontSize: '0.8em', color: '#9ca3af', marginLeft: 4 }}>(edited)</span>
                )}
              </p>
              <span>{new Date(msg.timestamp).toLocaleTimeString()}</span>
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, marginLeft: 6 }}>
                <button onClick={() => handleReplyClick(msg)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#10b981', padding: 0 }} title="Reply">
                  <Reply size={18} />
                </button>
                {msg.senderId === senderId && (
                  <button
                    onClick={() => {
                      setInputMessage(msg.content);
                      setEditingMsgId(msg.id);
                      setReplyToMessage(null);
                      setTimeout(() => {
                        document.getElementById('chat-input')?.focus();
                        document.getElementById('chat-input')?.scrollIntoView({ behavior: 'smooth', block: 'end' });
                      }, 50);
                    }}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#10b981', padding: 0 }}
                    title="Edit"
                  >
                    <Pencil size={18} />
                  </button>
                )}
              </span>
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