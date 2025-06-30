import { useState, useEffect, useRef, useCallback } from 'react';
import { messageService } from '../services/messageService';
import { documentService } from '../services/documentService';
import '../styles/MessageList.css';

import { GoPencil } from "react-icons/go";
import { FaReply } from "react-icons/fa";
import { FaPaperclip } from "react-icons/fa";
import { FaDownload } from "react-icons/fa";
import { FaEye } from "react-icons/fa";

const AGENT = {
  email: 'telebanquier@soge.ma',
  fullName: 'Télébanquier 1'
};

const MessageList = ({ conversationId }) => {
  const [messages, setMessages] = useState([]);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const messagesEndRef = useRef(null);
  const observer = useRef();
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const messagesContainerRef = useRef(null);
  const prevHeightRef = useRef(0);
  const [editingId, setEditingId] = useState(null);
  const [editingContent, setEditingContent] = useState("");
  const [editingLoading, setEditingLoading] = useState(false);
  const [replyTo, setReplyTo] = useState(null);
  const messageRefs = useRef({});
  const [highlightedId, setHighlightedId] = useState(null);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef(null);
  const [downloading, setDownloading] = useState(false);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewUrls, setPreviewUrls] = useState({});
  const [currentPreviewType, setCurrentPreviewType] = useState(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  const lastMessageElementRef = useCallback(node => {
    if (loading) return;
    if (observer.current) observer.current.disconnect();
    observer.current = new IntersectionObserver(entries => {
      if (entries[0].isIntersecting && hasMore) {
        setPage(prevPage => prevPage + 1);
      }
    });
    if (node) observer.current.observe(node);
  }, [loading, hasMore]);

  useEffect(() => {
    if (conversationId) {
      setMessages([]);
      setPage(0);
      setHasMore(true);
    }
  }, [conversationId]);

  useEffect(() => {
    const fetchMessages = async () => {
      if (!conversationId) return;
      
      try {
        setLoading(true);
        const response = await messageService.getMessages(conversationId, page);
        setMessages(prevMessages => {
          if (page === 0) return response.messages;
          return [...prevMessages, ...response.messages];
        });
        setHasMore(!response.isLast);
      } catch (error) {
        console.error('Error loading messages:', error);
      } finally {
        setLoading(false);
      }
    }; 

    fetchMessages();
  }, [conversationId, page]);

  useEffect(() => {
    if (page > 0 && messagesContainerRef.current) {
      prevHeightRef.current = messagesContainerRef.current.scrollHeight;
    }
  }, [page]);

  useEffect(() => {
    if (page > 0 && messagesContainerRef.current) {
      const container = messagesContainerRef.current;
      container.scrollTop = container.scrollHeight - prevHeightRef.current;
    }
  }, [messages]);

  useEffect(() => {
    if (page === 0 && messagesContainerRef.current) {
      messagesContainerRef.current.scrollTop = messagesContainerRef.current.scrollHeight;
    }
  }, [messages, page]);

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('en-GB', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  };

  const handleSend = async () => {
    if (!input.trim() || !conversationId) return;
    setSending(true);
    try {
      const messageData = {
        content: input,
        attachmentId: null,
        agent: AGENT,
        conversationId
      };
      let sentMessage;
      if (replyTo) {
        sentMessage = await messageService.replyTo(replyTo.id, messageData);
      } else {
        sentMessage = await messageService.sendMessage(messageData);
      }
      setMessages(prev => [sentMessage, ...prev]);
      setInput("");
      setReplyTo(null);
      scrollToBottom();
    } catch (error) {
      console.error('Error sending message:', error);
    } finally {
      setSending(false);
    }
  };

  const handleInputKeyDown = (e) => {
    if (e.key === 'Enter' && e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleEditClick = (message) => {
    setEditingId(message.id);
    setEditingContent(message.content);
  };

  const handleEditChange = (e) => {
    setEditingContent(e.target.value);
  };

  const handleEditSave = async (messageId) => {
    if (!editingContent.trim()) return;
    setEditingLoading(true);
    try {
      const updated = await messageService.editMessage(messageId, editingContent);
      setMessages(prev => prev.map(m => m.id === messageId ? { ...m, content: updated.content } : m));
      setEditingId(null);
      setEditingContent("");
    } catch (error) {
      
    } finally {
      setEditingLoading(false);
    }
  };

  const handleEditKeyDown = (e, messageId) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleEditSave(messageId);
    } else if (e.key === 'Escape') {
      setEditingId(null);
      setEditingContent("");
    }
  };

  const handleReplyClick = (message) => {
    setReplyTo(message);
  };

  const handleCancelReply = () => {
    setReplyTo(null);
  };

  const handleReplyPreviewClick = (repliedToId) => {
    const ref = messageRefs.current[repliedToId];
    if (ref && ref.scrollIntoView) {
      ref.scrollIntoView({ behavior: 'smooth', block: 'center' });
      setHighlightedId(repliedToId);
      setTimeout(() => setHighlightedId(null), 1200);
    }
  };

  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file || !conversationId) return;

    setUploading(true);
    try {
      const uploadResponse = await documentService.uploadFile(file);
      
      const messageData = {
        content: `📎 ${file.name}`,
        attachmentId: uploadResponse.id,
        agent: AGENT,
        conversationId
      };

      let sentMessage;
      if (replyTo) {
        sentMessage = await messageService.replyTo(replyTo.id, messageData);
      } else {
        sentMessage = await messageService.sendMessage(messageData);
      }

      setMessages(prev => [sentMessage, ...prev]);
      setReplyTo(null);
      scrollToBottom();
    } catch (error) {
      console.error('Error uploading file:', error);
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleDownload = async (attachmentId) => {
    if (!attachmentId) return;
    
    setDownloading(true);
    try {
      await documentService.downloadFile(attachmentId);
    } catch (error) {
      console.error('Error downloading file:', error);
    } finally {
      setDownloading(false);
    }
  };

  const handlePreview = async (message) => {
    if (!message.attachmentId) return;
    
    setPreviewLoading(true);
    try {
      const url = await documentService.previewFile(message.attachmentId);
      const fileType = message.content.toLowerCase().endsWith('.pdf') ? 'pdf' : 'image';
      setPreviewUrl(url);
      setCurrentPreviewType(fileType);
      setPreviewUrls(prev => ({ ...prev, [message.attachmentId]: { url, type: fileType } }));
    } catch (error) {
      console.error('Error previewing file:', error);
    } finally {
      setPreviewLoading(false);
    }
  };

  useEffect(() => {
    const loadImagePreviews = async () => {
      const messagesWithAttachments = messages.filter(message => message.attachmentId && (
        message.content.toLowerCase().endsWith('.jpg') ||
        message.content.toLowerCase().endsWith('.jpeg') ||
        message.content.toLowerCase().endsWith('.png') ||
        message.content.toLowerCase().endsWith('.gif') ||
        message.content.toLowerCase().endsWith('.pdf')
      ));

      console.log('Found attachment messages:', messagesWithAttachments);
      console.log('Current preview URLs:', previewUrls);

      const newPreviewUrls = { ...previewUrls };
      let hasNewUrls = false;

      for (const message of messagesWithAttachments) {
        if (!previewUrls[message.attachmentId]) {
          try {
            const fileType = message.content.toLowerCase().endsWith('.pdf') ? 'pdf' : 'image';
            console.log(`Loading preview for message: ${message.id}, attachmentId: ${message.attachmentId}, type: ${fileType}`);
            const url = await documentService.previewFile(message.attachmentId);
            newPreviewUrls[message.attachmentId] = { url, type: fileType };
            hasNewUrls = true;
            console.log('Successfully loaded preview for message:', message.id);
          } catch (error) {
            console.error('Error loading preview for message:', message.id, error);
          }
        }
      }

      if (hasNewUrls) {
        console.log('Updating preview URLs with new values:', newPreviewUrls);
        setPreviewUrls(newPreviewUrls);
      }
    };

    loadImagePreviews();
  }, [messages, previewUrls]);

  const closePreview = () => {
    if (previewUrl) {
      window.URL.revokeObjectURL(previewUrl);
      setPreviewUrl(null);
      setCurrentPreviewType(null);
    }
  };

  if (!conversationId) {
    return (
      <div className="message-list-empty">
        <p>Select a conversation to view messages</p>
      </div>
    );
  }

  return (
    <div className="message-list-wrapper">
      <div className="message-list" ref={messagesContainerRef}>
        {loading && <div className="loading-spinner">Loading...</div>}
        {messages.slice().reverse().map((message, index, arr) => {
          const isCustomer = message.customer !== null;
          const sender = isCustomer ? message.customer : message.agent;
          const isFirstRendered = index === 0;
          const isAgent = !isCustomer && sender && sender.email === AGENT.email;
          const hasAttachment = message.attachmentId !== null && message.attachmentId !== undefined;
          const isImage = hasAttachment && 
            (message.content.toLowerCase().endsWith('.jpg') || 
             message.content.toLowerCase().endsWith('.jpeg') || 
             message.content.toLowerCase().endsWith('.png') || 
             message.content.toLowerCase().endsWith('.gif'));
          const isPdf = hasAttachment && 
            message.content.toLowerCase().endsWith('.pdf');
          
          return (
            <div
              key={message.id}
              ref={el => { messageRefs.current[message.id] = el; if (isFirstRendered) lastMessageElementRef(el); }}
              className={`message-container ${isCustomer ? 'customer' : 'agent'}${highlightedId === message.id ? ' highlight' : ''}`}
            >
              <div className="message-content">
                <div className="message-header">
                  <span className="message-sender">{sender.fullName}</span>
                  <div className="message-actions">
                    {hasAttachment && (
                      <>
                        <button 
                          className="preview-btn" 
                          onClick={() => handlePreview(message)} 
                          title="Preview file"
                          disabled={previewLoading}
                        >
                          <FaEye />
                        </button>
                        <button 
                          className="download-btn" 
                          onClick={() => handleDownload(message.attachmentId)} 
                          title="Download file"
                          disabled={downloading}
                        >
                          <FaDownload />
                        </button>
                      </>
                    )}
                    {isAgent && editingId !== message.id && (
                      <button className="edit-btn" onClick={() => handleEditClick(message)} title="Edit message"><GoPencil /></button>
                    )}
                    {editingId !== message.id && (
                      <button className="reply-btn" onClick={() => handleReplyClick(message)} title="Reply to message"><FaReply /></button>
                    )}
                  </div>
                </div>
                {message.repliedTo && (
                  <div
                    className="bubble-reply-preview clickable"
                    onClick={() => handleReplyPreviewClick(message.repliedTo.id)}
                    title="Go to replied message"
                  >
                    <span className="bubble-reply-sender">
                      {message.repliedTo.agent?.fullName || message.repliedTo.customer?.fullName || 'Unknown'}
                    </span>
                    <span className="bubble-reply-content">
                      {message.repliedTo.content}
                    </span>
                  </div>
                )}
                {editingId === message.id ? (
                  <div className="edit-message-area">
                    <input
                      type="text"
                      className="edit-message-input"
                      value={editingContent}
                      onChange={handleEditChange}
                      onKeyDown={e => handleEditKeyDown(e, message.id)}
                      disabled={editingLoading}
                      autoFocus
                    />
                    <button
                      className="save-edit-btn"
                      onClick={() => handleEditSave(message.id)}
                      disabled={editingLoading || !editingContent.trim()}
                    >
                      Save
                    </button>
                    <button
                      className="cancel-edit-btn"
                      onClick={() => { setEditingId(null); setEditingContent(""); }}
                      disabled={editingLoading}
                    >
                      Cancel
                    </button>
                  </div>
                ) : (
                  <>
                    <div className="message-text">{message.content}</div>
                    {hasAttachment && (
                      <div className="attachment-preview">
                        {isImage ? (
                          previewUrls[message.attachmentId]?.url ? (
                            <img 
                              src={previewUrls[message.attachmentId].url}
                              alt="Attachment preview"
                              className="attachment-image"
                              onClick={() => handlePreview(message)}
                            />
                          ) : (
                            <div className="attachment-loading">
                              Loading preview...
                            </div>
                          )
                        ) : isPdf ? (
                          <div 
                            className="attachment-pdf"
                            onClick={() => handlePreview(message)}
                          >
                            <div className="pdf-icon">📄</div>
                            <span className="pdf-name">
                              {message.content.startsWith('📎') ? message.content.replace('📎 ', '') : message.content}
                            </span>
                          </div>
                        ) : (
                          <div className="attachment-file">
                            <FaPaperclip className="attachment-icon" />
                            <span className="attachment-name">
                              {message.content.startsWith('📎') ? message.content.replace('📎 ', '') : message.content}
                            </span>
                          </div>
                        )}
                      </div>
                    )}
                  </>
                )}
                <span className={`message-time-bubble ${isCustomer ? 'left' : 'right'}`}>{formatDate(message.createdAt)}</span>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>
      {previewUrl && (
        <div className="preview-modal" onClick={closePreview}>
          <div className="preview-content" onClick={e => e.stopPropagation()}>
            <button className="close-preview-btn" onClick={closePreview}>✕</button>
            {currentPreviewType === 'pdf' ? (
              <iframe src={previewUrl} className="preview-iframe" />
            ) : (
              <img src={previewUrl} alt="Preview" className="preview-image" />
            )}
          </div>
        </div>
      )}
      {conversationId && (
        <form className="message-input-area" onSubmit={e => { e.preventDefault(); handleSend(); }}>
          {replyTo && (
            <div className="reply-preview">
              <span>Replying to: </span>
              <span className="reply-preview-content">{replyTo.content}</span>
              <button type="button" className="cancel-reply-btn" onClick={handleCancelReply}>✕</button>
            </div>
          )}
          <div className="message-input-container">
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileUpload}
              style={{ display: 'none' }}
              disabled={uploading}
            />
            <button
              type="button"
              className="attach-file-btn"
              onClick={() => fileInputRef.current?.click()}
              disabled={uploading}
              title="Attach file"
            >
              <FaPaperclip />
            </button>
            <textarea
              className="message-input"
              placeholder="Type a message..."
              value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={handleInputKeyDown}
              disabled={sending || uploading}
              autoFocus
              rows={1}
            />
            <button type="submit" className="send-button" disabled={sending || uploading || !input.trim()}>
              Send
            </button>
          </div>
        </form>
      )}
    </div>
  );
};

export default MessageList; 