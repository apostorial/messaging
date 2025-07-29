import { useEffect } from 'react'
import './App.css'
import { useAgentStore } from './stores/agent-store'
import { findOrCreate } from './lib/services/agent-service'
import ConversationList from './components/ConversationList'

function App() {
  const { agent, setAgent } = useAgentStore()

  useEffect(() => {
    findOrCreate({ fullName: 'Télé-banquier', email: 'tele-banquier@soge.ma' })
      .then(setAgent)
  }, [])

  return (
    <>
      <ConversationList />
    </>
  )
}

export default App
