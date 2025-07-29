import { useEffect, useRef, useCallback, useState } from 'react'
import { findAll } from '../lib/services/conversation-service'
import { useConversationStore } from '../stores/conversation-store'
import logo from '../assets/logo.svg'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { UserRound } from 'lucide-react'
import ChatView from './ChatView'
import type { ConversationResponse } from '../types/conversation'

function ConversationList() {
  const { conversations, setConversations, appendConversations } = useConversationStore()
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(false)
  const [selectedConversation, setSelectedConversation] = useState<ConversationResponse | null>(null)
  const observer = useRef<IntersectionObserver | undefined>(undefined)

  const loadConversations = useCallback(async (pageNum: number) => {
    setLoading(true)
    try {
      const response = await findAll(pageNum)
      if (pageNum === 0) {
        setConversations(response.content)
      } else {
        appendConversations(response.content)
      }
      setHasMore(!response.last)
    } catch (error) {
      console.error('Error loading conversations:', error)
    } finally {
      setLoading(false)
    }
  }, [setConversations, appendConversations])

  const lastConversationElementRef = useCallback((node: HTMLDivElement) => {
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
    loadConversations(page)
  }, [page, loadConversations])

  useEffect(() => {
    loadConversations(0);
    
    const socket = new SockJS(import.meta.env.VITE_API_BASE_URL + '/ws');
    const client = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        onConnect: () => {
            console.log("STOMP connected");
          client.subscribe('/topic/conversation-updates', (message) => {
            console.log(message.body);
            console.log("Update received");
            loadConversations(0);
          });
        }
      });
      client.activate();
  }, [loadConversations])

  return (
    <div className="flex h-screen">
      <div className="w-80 bg-white border-r border-gray-200 flex flex-col">
      <div className="bg-gray-50 px-4 py-3 border-b border-gray-200 flex items-center space-x-3">
        <img src={logo} alt="Logo" className="h-12 mx-auto" />
      </div>

      <div className="flex-1 overflow-y-auto">
        {conversations.map((conversation, index) => (
          <div 
            key={conversation.id} 
            ref={index === conversations.length - 1 ? lastConversationElementRef : null}
            className="px-2 py-3 hover:bg-gray-50 cursor-pointer border-b border-gray-100 transition-colors"
            onClick={() => setSelectedConversation(conversation)}
          >
            <div className="flex items-center space-x-0">
              <div className="w-12 h-12 flex items-center justify-center text-gray-600">
                <UserRound size={24} />
              </div>
              
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-gray-900 truncate">
                    {conversation.owner.fullName}
                  </h3>
                  <span className="text-xs text-gray-500">
                    {conversation.unreadCount > 0 && (
                      <span className="bg-blue-500 text-white rounded-full px-2 py-1 text-xs font-medium">
                        {conversation.unreadCount}
                      </span>
                    )}
                  </span>
                </div>
                
                <div className="flex items-center justify-between mt-1">
                  <p className="text-sm text-gray-600 truncate flex-1">
                    {conversation.lastMessageSender && (
                      <span className="font-medium text-gray-700">{conversation.lastMessageSender}: </span>
                    )}
                    {conversation.lastMessageContent || 'No messages yet'}
                  </p>
                  {conversation.lastUpdated && (
                    <span className="text-xs text-gray-400 ml-2 flex-shrink-0">
                      {new Date(conversation.lastUpdated).toLocaleTimeString('en-GB', { 
                        hour: '2-digit', 
                        minute: '2-digit',
                        hour12: false
                      })}
                    </span>
                  )}
                </div>
              </div>
            </div>
          </div>
        ))}
        
        {conversations.length === 0 && !loading && (
          <div className="flex items-center justify-center h-full text-gray-500">
            <p>No conversations yet</p>
          </div>
        )}
        
        {loading && (
          <div className="flex items-center justify-center py-4 text-gray-500">
            <p>Loading...</p>
          </div>
        )}
      </div>
      </div>

      <div className="flex-1">
        {selectedConversation ? (
          <ChatView 
            conversation={selectedConversation} 
            onBack={() => setSelectedConversation(null)} 
          />
        ) : (
          <div className="flex items-center justify-center h-full bg-gray-50">
            <div className="text-center">
              <UserRound size={48} className="mx-auto text-gray-400 mb-4" />
              <p className="text-gray-500">Select a conversation to start messaging</p>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default ConversationList
