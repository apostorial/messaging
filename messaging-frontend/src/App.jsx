import './App.css';
import AgentChat from './AgentChat';
import CustomerChat from './CustomerChat';

function App() {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-around', padding: '20px', gap: '20px' }}>
      <div>
        <h2>Agent View</h2>
        <AgentChat />
      </div>
      <div>
        <h2>Customer View</h2>
        <CustomerChat />
      </div>
    </div>
  );
}

export default App;
