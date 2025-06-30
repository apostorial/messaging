import { useState, useEffect, useRef, useCallback } from 'react';
import { conversationService } from '../services/conversationService';
import '../styles/ConversationList.css';
import { FaUser, FaUserAlt, FaUserTie } from 'react-icons/fa';

const ConversationList = ({ onSelectConversation }) => {
  const [conversations, setConversations] = useState([]);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const observer = useRef();

  const userIcons = [
    FaUser,
    FaUserAlt,
    FaUserTie,
  ];

  const getRandomIcon = (id) => {
    const index = id.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) % userIcons.length;
    return userIcons[index];
  };onSelectConversation

  const lastConversationElementRef = useCallback(node => {
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
    const fetchConversations = async () => {
      try {
        setLoading(true);
        const response = await conversationService.getConversations(page);
        setConversations(prevConversations => {
          if (page === 0) return response.conversations;
          return [...prevConversations, ...response.conversations];
        });
        setHasMore(!response.isLast);
      } catch (error) {
        console.error('Error loading conversations:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchConversations();
  }, [page]);

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handleClick = (conversationId) => {
    if (onSelectConversation) {
      onSelectConversation(conversationId);
    }
  };

  return (
    <div className="conversation-list">
      {conversations.map((conversation, index) => {
        const Icon = getRandomIcon(conversation.id);
        return (
          <div
            key={conversation.id}
            ref={index === conversations.length - 1 ? lastConversationElementRef : null}
            className="conversation-item"
            onClick={() => handleClick(conversation.id)}
          >
            <Icon className="conversation-icon" />
            <div className="conversation-content">
              <div className="conversation-title">{conversation.customer.fullName}</div>
              <div className="conversation-preview">
                <span className="conversation-date">{formatDate(conversation.updatedAt)}</span>
                <span className="conversation-id">ID: {conversation.customer.prospectId}</span>
              </div>
            </div>
          </div>
        );
      })}
      {loading && <div className="loading-spinner">Loading...</div>}
    </div>
  );
};

export default ConversationList; 