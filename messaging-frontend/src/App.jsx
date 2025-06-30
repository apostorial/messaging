import { useState } from 'react'
import './App.css'
import ConversationList from './components/ConversationList'
import MessageList from './components/MessageList'

function App() {
  const [selectedConversationId, setSelectedConversationId] = useState(null);

  return (
    <div className="app">
      <ConversationList onSelectConversation={setSelectedConversationId} />
      <MessageList conversationId={selectedConversationId} />
    </div>
  )
}

export default App
