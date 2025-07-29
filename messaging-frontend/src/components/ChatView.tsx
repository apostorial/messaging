import { useEffect, useRef, useCallback, useState } from 'react'
import { findAll, send, markAsRead, editMessage } from '../lib/services/message-service'
import { useMessageStore } from '../stores/message-store'
import { useAgentStore } from '../stores/agent-store'
import type { ConversationResponse } from '../types/conversation'
import { UserRound, Send, Paperclip, Check, CheckCheck, Edit2, X, Check as CheckIcon, Reply, ArrowLeft } from 'lucide-react'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

interface ChatViewProps {
  conversation: ConversationResponse | null
  onBack: () => void
}

function ChatView({ conversation }: ChatViewProps) {
  const { messages, setMessages, appendMessages, clearMessages, markMessagesAsRead, editMessage: editMessageInStore } = useMessageStore()
  const { agent } = useAgentStore()
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(false)
  const [sending, setSending] = useState(false)
  const [messageText, setMessageText] = useState('')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [zoomedImage, setZoomedImage] = useState<string | null>(null)
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null)
  const [editText, setEditText] = useState('')
  const [replyToMessage, setReplyToMessage] = useState<any>(null)
  const [highlightedMessageId, setHighlightedMessageId] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const editInputRef = useRef<HTMLInputElement>(null)
  const observer = useRef<IntersectionObserver | undefined>(undefined)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const messageRefs = useRef<{ [key: string]: HTMLDivElement | null }>({})

  const loadMessages = useCallback(async (pageNum: number) => {
    if (!conversation) return
    
    setLoading(true)
    try {
      const response = await findAll(conversation.id, pageNum)
      if (pageNum === 0) {
        setMessages(response.content)
      } else {
        appendMessages(response.content)
      }
      setHasMore(!response.last)
    } catch (error) {
      console.error('Error loading messages:', error)
    } finally {
      setLoading(false)
    }
  }, [conversation, setMessages, appendMessages])

  const firstMessageElementRef = useCallback((node: HTMLDivElement) => {
    if (loading) return
    if (observer.current) observer.current.disconnect()
    observer.current = new IntersectionObserver(entries => {
      if (entries[0].isIntersecting && hasMore) {
        setPage(prevPage => prevPage + 1)
      }
    })
    if (node) observer.current.observe(node)
  }, [loading, hasMore])

  useEffect(() => {
    if (conversation) {
      clearMessages()
      setPage(0)
      setHasMore(true)
      loadMessages(0)
      
      markAsRead(conversation.id).catch(error => {
        console.error('Error marking conversation as read:', error);
      });
      
      const socket = new SockJS(import.meta.env.VITE_API_BASE_URL + '/ws');
      const client = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        onConnect: () => {
          console.log("STOMP connected for messages");
          
          client.subscribe(`/topic/conversation${conversation.id}`, (message) => {
            console.log("New message received:", message.body);
            try {
              const newMessage = JSON.parse(message.body);
              const existingMessageIndex = messages.findIndex(m => m.id === newMessage.id);
              if (existingMessageIndex !== -1) {
                setMessages(messages.map((msg, index) => 
                  index === existingMessageIndex ? newMessage : msg
                ));
              } else {
                appendMessages([newMessage]);
              }
            } catch (error) {
              console.error("Error parsing message:", error);
              loadMessages(0);
            }
          });
          
          client.subscribe(`/topic/conversation/${conversation.id}/read`, (message) => {
            console.log("Read status update received:", message.body);
            try {
              const readMessageIds = JSON.parse(message.body);
              markMessagesAsRead(readMessageIds);
            } catch (error) {
              console.error("Error parsing read status update:", error);
            }
          });
        }
      });
      client.activate();
      
      return () => {
        client.deactivate();
      };
    }
  }, [conversation, clearMessages, loadMessages, markMessagesAsRead])

  useEffect(() => {
    loadMessages(page)
    
  }, [page, loadMessages])

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (file) {
      setSelectedFile(file)
    }
  }

  const handleRemoveFile = () => {
    setSelectedFile(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const handleImageClick = (imageUrl: string) => {
    setZoomedImage(imageUrl)
  }

  const handleCloseZoom = () => {
    setZoomedImage(null)
  }

  const handleStartEdit = (message: any) => {
    setEditingMessageId(message.id)
    setEditText(message.content)
  }

  const handleCancelEdit = () => {
    setEditingMessageId(null)
    setEditText('')
  }

  const handleReplyToMessage = (message: any) => {
    setReplyToMessage(message)
  }

  const handleCancelReply = () => {
    setReplyToMessage(null)
  }

  const handleReplyPreviewClick = (messageId: string) => {
    const messageElement = messageRefs.current[messageId]
    if (messageElement) {
      const messagesContainer = messageElement.closest('.overflow-y-auto')
      if (messagesContainer) {
        const containerRect = messagesContainer.getBoundingClientRect()
        const elementRect = messageElement.getBoundingClientRect()
        const scrollTop = messagesContainer.scrollTop
        
        const targetScrollTop = scrollTop + elementRect.top - containerRect.top - 20
        
        messagesContainer.scrollTo({
          top: targetScrollTop,
          behavior: 'smooth'
        })
      }
      
      setHighlightedMessageId(messageId)
      setTimeout(() => {
        setHighlightedMessageId(null)
      }, 2000)
    }
  }

  const handleSaveEdit = async () => {
    if (!editingMessageId || !editText.trim()) return
    
    try {
      await editMessage(editingMessageId, editText.trim())
      const originalMessage = messages.find(m => m.id === editingMessageId)
      if (originalMessage) {
        editMessageInStore(editingMessageId, editText.trim())
      }
      setEditingMessageId(null)
      setEditText('')
    } catch (error) {
      console.error('Error editing message:', error)
    }
  }

  const handleEditKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSaveEdit()
    } else if (e.key === 'Escape') {
      handleCancelEdit()
    }
  }

  const markVisibleMessagesAsRead = useCallback(() => {
    if (!conversation) return
    
    const unreadCustomerMessages = messages.filter(
      message => !message.read && message.senderType === 'CUSTOMER'
    )
    
    console.log('Unread customer messages found:', unreadCustomerMessages.length)
    console.log('All messages:', messages.map(m => ({ id: m.id, read: m.read, senderType: m.senderType })))
    
    if (unreadCustomerMessages.length > 0) {
      const messageIds = unreadCustomerMessages.map(message => message.id)
      console.log('Marking messages as read:', messageIds)
      markMessagesAsRead(messageIds)
      
      markAsRead(conversation.id).catch(error => {
        console.error('Error marking messages as read:', error)
      })
    }
  }, [conversation, messages, markMessagesAsRead])

  const handleSendMessage = async () => {
    if ((!messageText.trim() && !selectedFile) || !agent || !conversation) return
    
    setSending(true)
    try {
      await send({
        content: messageText.trim(),
        agentId: agent.id,
        senderType: 'AGENT',
        conversationId: conversation.id,
        replyToId: replyToMessage?.id
      }, selectedFile || undefined)
      
      setMessageText('')
      setSelectedFile(null)
      setReplyToMessage(null)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    } catch (error) {
      console.error('Error sending message:', error)
    } finally {
      setSending(false)
    }
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  useEffect(() => {
    const messagesContainer = messagesEndRef.current?.parentElement
    if (messagesContainer) {
      const isNearBottom = messagesContainer.scrollHeight - messagesContainer.scrollTop <= messagesContainer.clientHeight + 100
      
      if (isNearBottom) {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
      }
    }
  }, [messages])

  useEffect(() => {
    console.log('Checking for unread messages:', messages.length)
    markVisibleMessagesAsRead()
  }, [messages, markVisibleMessagesAsRead])

  if (!conversation) {
    return null
  }

  return (
    <div className="flex-1 flex flex-col h-screen bg-gray-50">
      <div className="bg-white px-4 py-3 border-b border-gray-200 flex items-center space-x-3">
        <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center text-white">
          <UserRound size={16} />
        </div>
        <div>
          <h2 className="text-sm font-semibold text-gray-900">
            {conversation.owner.fullName}
          </h2>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {loading && (
          <div className="flex justify-center">
            <p className="text-sm text-gray-500">Loading...</p>
          </div>
        )}
        
        {messages.map((message, index) => {
          const isCustomer = message.senderType === 'CUSTOMER'
          const isFirstMessage = index === 0
          const hasImage = message.fileUrl;
          const hasReply = message.replyTo;
          const isHighlighted = highlightedMessageId === message.id;
          
          return (
            <div
              key={message.id}
              ref={(el) => {
                if (isFirstMessage) {
                  firstMessageElementRef(el!);
                }
                messageRefs.current[message.id] = el;
              }}
              className={`flex ${isCustomer ? 'justify-start' : 'justify-end'} transition-all duration-300 ${
                isHighlighted ? 'scale-[1.02]' : 'scale-100'
              }`}
            >
              <div className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg transition-all duration-300 ${
                isCustomer 
                  ? 'bg-white border border-gray-200' 
                  : 'bg-blue-500 text-white'
              } ${
                isHighlighted 
                  ? isCustomer 
                    ? 'shadow-lg ring-2 ring-blue-300 bg-blue-50' 
                    : 'shadow-lg ring-2 ring-blue-200 bg-blue-400'
                  : ''
              }`}>
                {hasReply && (
                  <button
                    onClick={() => handleReplyPreviewClick(hasReply.id)}
                    className={`mb-2 p-2 rounded text-xs w-full text-left hover:opacity-80 transition-opacity ${
                      isCustomer 
                        ? 'bg-gray-100 text-gray-600 border-l-3 border-gray-400' 
                        : 'bg-blue-400 text-blue-100 border-l-3 border-blue-200'
                    }`}
                  >
                    <div className="flex items-center space-x-1 mb-1">
                      <ArrowLeft size={10} />
                      <span className="font-medium">
                        {hasReply.senderType === 'CUSTOMER' ? 'Customer' : 'You'}
                      </span>
                    </div>
                    <div className="truncate">
                      {hasReply.content || (hasReply.fileUrl ? '📷 Image' : '')}
                    </div>
                  </button>
                )}
                
                {hasImage && (
                  <div className="mb-2">
                    <img 
                      src={message.fileUrl} 
                      alt="Attachment"
                      className="max-w-full h-auto rounded-lg cursor-pointer hover:opacity-90 transition-opacity"
                      onClick={() => handleImageClick(message.fileUrl)}
                    />
                  </div>
                )}
                {message.content ? (
                  editingMessageId === message.id ? (
                    <div className="flex items-center space-x-2">
                      <input
                        ref={editInputRef}
                        type="text"
                        value={editText}
                        onChange={(e) => setEditText(e.target.value)}
                        onKeyPress={handleEditKeyPress}
                        className="flex-1 px-2 py-1 text-sm border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-blue-500"
                        autoFocus
                      />
                      <button
                        onClick={handleSaveEdit}
                        className="p-1 text-green-600 hover:text-green-800"
                      >
                        <CheckIcon size={14} />
                      </button>
                      <button
                        onClick={handleCancelEdit}
                        className="p-1 text-red-600 hover:text-red-800"
                      >
                        <X size={14} />
                      </button>
                    </div>
                  ) : (
                    <div className="flex items-center justify-between">
                      <p className="text-sm">{message.content}</p>
                      <div className="flex items-center space-x-1 ml-2">
                        <button
                          onClick={() => handleReplyToMessage(message)}
                          className={`p-1 transition-colors ${
                            isCustomer 
                              ? 'text-gray-400 hover:text-gray-600' 
                              : 'text-blue-100 hover:text-white'
                          }`}
                        >
                          <Reply size={12} />
                        </button>
                        {!isCustomer && (
                          <button
                            onClick={() => handleStartEdit(message)}
                            className="p-1 text-white hover:text-gray-200 transition-colors"
                          >
                            <Edit2 size={12} />
                          </button>
                        )}
                      </div>
                    </div>
                  )
                ) : (
                  <div className="flex items-center justify-end space-x-1 mt-2">
                    <button
                      onClick={() => handleReplyToMessage(message)}
                      className={`p-1 transition-colors ${
                        isCustomer 
                          ? 'text-gray-400 hover:text-gray-600' 
                          : 'text-blue-100 hover:text-white'
                      }`}
                    >
                      <Reply size={12} />
                    </button>
                    {!isCustomer && (
                      <button
                        onClick={() => handleStartEdit(message)}
                        className="p-1 text-white hover:text-gray-200 transition-colors"
                      >
                        <Edit2 size={12} />
                      </button>
                    )}
                  </div>
                )}
                <div className={`flex items-center justify-between mt-1 ${
                  isCustomer ? 'text-gray-500' : 'text-blue-100'
                }`}>
                  <span className="text-xs">
                    {new Date(message.timestamp).toLocaleTimeString('en-GB', { 
                      hour: '2-digit', 
                      minute: '2-digit',
                      hour12: false
                    })}
                  </span>
                  {!isCustomer && (
                    <div className="flex items-center space-x-1 ml-2">
                      {message.read ? (
                        <CheckCheck size={14} className="text-blue-100" />
                      ) : (
                        <Check size={14} className="text-blue-200" />
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )
        })}
        
        <div ref={messagesEndRef} />
      </div>

      <div className="bg-white px-4 py-3 border-t border-gray-200">
        {replyToMessage && (
          <div className="mb-2 p-2 bg-blue-50 border-l-4 border-blue-500 rounded-r-lg">
            <div className="flex items-center justify-between">
              <div className="flex-1 min-w-0">
                <div className="flex items-center space-x-2 mb-1">
                  <Reply size={12} className="text-blue-500" />
                  <span className="text-xs font-medium text-blue-700">
                    Replying to {replyToMessage.senderType === 'CUSTOMER' ? 'Customer' : 'You'}
                  </span>
                </div>
                <button
                  onClick={() => handleReplyPreviewClick(replyToMessage.id)}
                  className="text-sm text-gray-700 truncate hover:text-blue-600 transition-colors cursor-pointer text-left w-full"
                >
                  {replyToMessage.content || (replyToMessage.fileUrl ? '📷 Image' : '')}
                </button>
              </div>
              <button
                onClick={handleCancelReply}
                className="ml-2 p-1 text-gray-400 hover:text-gray-600"
              >
                <X size={14} />
              </button>
            </div>
          </div>
        )}
        
        {selectedFile && (
          <div className="mb-2 p-2 bg-gray-50 rounded-lg flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Paperclip size={16} className="text-gray-500" />
              <span className="text-sm text-gray-700 truncate">{selectedFile.name}</span>
            </div>
            <button
              onClick={handleRemoveFile}
              className="text-red-500 hover:text-red-700 text-sm"
            >
              Remove
            </button>
          </div>
        )}
        
        <div className="flex items-center space-x-2">
          <input
            ref={fileInputRef}
            type="file"
            onChange={handleFileSelect}
            className="hidden"
            accept="image/*,video/*,audio/*,.pdf,.doc,.docx,.txt"
          />
          <button
            onClick={() => fileInputRef.current?.click()}
            disabled={sending}
            className="p-2 text-gray-500 hover:text-gray-700 transition-colors disabled:opacity-50"
          >
            <Paperclip size={16} />
          </button>
          <input
            type="text"
            value={messageText}
            onChange={(e) => setMessageText(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Type a message..."
            disabled={sending}
            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-50"
          />
          <button 
            onClick={handleSendMessage}
            disabled={(!messageText.trim() && !selectedFile) || sending}
            className="p-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Send size={16} />
          </button>
        </div>
      </div>

      {zoomedImage && (
        <div 
          className="fixed inset-0 bg-black/75 flex items-center justify-center z-50"
          onClick={handleCloseZoom}
        >
          <div className="relative max-w-4xl max-h-full p-4">
            <img 
              src={zoomedImage} 
              alt="Zoomed image"
              className="max-w-full max-h-full object-contain rounded-lg"
              onClick={(e) => e.stopPropagation()}
            />
          </div>
        </div>
      )}
    </div>
  )
}

export default ChatView 