import { useEffect, useState, useRef } from 'react'
import { findByClientId } from '../lib/services/customer-service'
import ChatView from './ChatView'
import { User, FileText, MessageCircle, Calendar, Mail, Phone, MapPin } from 'lucide-react'
import Logo from '../assets/logo.png'
import ProfilePlaceholder from '../assets/profile-placeholder.svg'
import FrontIdImage from '../assets/front.jpg'
import BackIdImage from '../assets/back.jpg'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

interface BankCustomerViewProps {
  clientId: string
}

type TabType = 'data' | 'documents' | 'chat'

function BankCustomerView({ clientId }: BankCustomerViewProps) {
  const [customer, setCustomer] = useState<any>(null)
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<TabType>('data')
  const [error, setError] = useState<string | null>(null)
  const [unreadMessageCount, setUnreadMessageCount] = useState(0)
  const stompClientRef = useRef<Client | null>(null)
  const activeTabRef = useRef<TabType>('data')

  const bankCustomerData = {
    fullName: customer?.fullName || '',
    dateOfBirth: '1985-03-15',
    email: 'amine.bennani@email.com',
    nationality: 'Moroccan',
    nationalId: 'AB123456789',
    phoneNumber: '+212 6 12 34 56 78',
    maritalStatus: 'Married',
    occupation: 'Software Engineer',
    address: {
      street: '123 Avenue Mohammed V',
      city: 'Casablanca',
      postalCode: '20000',
      country: 'Morocco'
    },
    clientId: clientId,
    accountNumber: '1234567890123456',
    accountType: 'Liberté',
    accountStatus: 'Active',
    creditScore: 750,
    monthlyIncome: 25000,
    employmentStatus: 'Employed',
    employer: 'Tech Solutions SARL',
    accountOpeningDate: '2020-01-15',
    lastTransactionDate: '2024-01-10',
    totalBalance: 125000,
    availableCredit: 50000,
    riskLevel: 'Low',
    kycStatus: 'Verified',
    preferredLanguage: 'Arabic',
    branch: 'Casablanca Central',
    relationshipManager: 'Fatima Alami',
    customerSince: '2020-01-15',
    totalTransactions: 1247,
    averageTransactionAmount: 2500,
    lastLoginDate: '2024-01-09',
    notificationPreferences: {
      sms: true,
      email: true,
      push: false
    }
  }

  useEffect(() => {
    const fetchCustomer = async () => {
      try {
        setLoading(true)
        const customerData = await findByClientId(clientId)
        console.log(customerData)
        
        const transformedCustomer = {
          ...customerData,
          conversation: {
            ...customerData.conversation,
            owner: {
              id: customerData.id,
              fullName: customerData.fullName,
              prospectId: customerData.prospectId,
              clientId: customerData.clientId,
              conversation: customerData.conversation
            }
          }
        }
        
        setCustomer(transformedCustomer)
      } catch (err) {
        console.error('Error fetching customer:', err)
        setError('Failed to load customer data')
        setCustomer({ fullName: 'Customer ' + clientId })
      } finally {
        setLoading(false)
      }
    }

    fetchCustomer()
  }, [clientId])

  useEffect(() => {
    if (!customer?.conversation?.id) {
      console.log("No customer conversation ID, skipping WebSocket setup")
      return
    }

    console.log("Setting up WebSocket for conversation:", customer.conversation.id)
    const socket = new SockJS(import.meta.env.VITE_API_BASE_URL + '/ws')
    const client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        console.log("STOMP connected for notifications")
        
        client.subscribe('/topic/conversation-updates', (message) => {
          console.log("Conversation update received:", message.body)
          try {
            const conversationId = message.body.replace(/"/g, '')
            console.log("Received conversation ID:", conversationId)
            console.log("Current customer conversation ID:", customer?.conversation?.id)
            console.log("IDs match?", conversationId === customer?.conversation?.id)
            
            if (customer?.conversation?.id && conversationId === customer.conversation.id) {
              console.log("New message notification for current customer!")
              console.log("Current active tab:", activeTabRef.current)
              
              if (activeTabRef.current !== 'chat') {
                setUnreadMessageCount(prev => {
                  const newCount = prev + 1
                  console.log(`Incrementing unread count to: ${newCount} (not in chat tab)`)
                  return newCount
                })
              } else {
                console.log("Already in chat tab - not incrementing count")
              }
            } else {
              console.log("Notification not for current customer")
            }
          } catch (error) {
            console.error("Error parsing conversation update:", error)
          }
        })
      },
      onDisconnect: () => {
        console.log("STOMP disconnected")
      }
    })
    
    client.activate()
    stompClientRef.current = client
    
    return () => {
      client.deactivate()
    }
  }, [customer?.conversation?.id])

  useEffect(() => {
    activeTabRef.current = activeTab
    console.log("Active tab changed to:", activeTab)
    if (activeTab === 'chat') {
      console.log("Clearing unread message count from", unreadMessageCount, "to 0")
      setUnreadMessageCount(0)
    }
  }, [activeTab])

  const tabs = [
    { id: 'data' as TabType, label: 'Customer Data', icon: User },
    { id: 'documents' as TabType, label: 'Documents', icon: FileText },
    { id: 'chat' as TabType, label: 'Chat', icon: MessageCircle }
  ]

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading customer data...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-screen bg-gray-50">
        <div className="text-center">
          <div className="text-red-500 text-6xl mb-4">⚠️</div>
          <p className="text-red-600 text-lg">{error}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="h-screen bg-gray-100 flex">
      <div className="w-80 bg-white border-r border-gray-200 flex flex-col">
        <div className="p-6 border-b border-gray-200 flex justify-center">
          <img src={Logo} alt="Bank Logo" className="h-12" />
        </div>

        <div className="flex-1 p-4">
          <nav className="space-y-2">
            {tabs.map((tab) => {
              const Icon = tab.icon
              const isChatTab = tab.id === 'chat'
              return (
                <button
                  key={tab.id}
                  onClick={() => {
                    setActiveTab(tab.id)
                    if (tab.id === 'chat') {
                      console.log("Chat tab clicked - clearing notification count")
                      setUnreadMessageCount(0)
                    }
                  }}
                  className={`w-full flex items-center space-x-3 px-4 py-3 rounded-lg text-left transition-colors relative ${
                    activeTab === tab.id
                      ? 'bg-gray-900 text-white'
                      : 'text-gray-700 hover:bg-gray-100'
                  }`}
                >
                  <Icon size={20} />
                  <span className="font-medium">{tab.label}</span>
                  {isChatTab && unreadMessageCount > 0 && (
                    <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                      <div className="bg-red-500 text-white text-xs font-bold rounded-full min-w-[20px] h-5 flex items-center justify-center px-1.5 animate-pulse">
                        {unreadMessageCount > 99 ? '99+' : unreadMessageCount}
                      </div>
                    </div>
                  )}
                </button>
              )
            })}
          </nav>
        </div>
      </div>

      <div className="flex-1 flex flex-col">
        <div className="flex-1 overflow-hidden">
        {activeTab === 'data' && (
          <div className="h-full overflow-y-auto p-6">
            <div className="max-w-6xl mx-auto space-y-6">
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <div className="flex items-center space-x-4 mb-6">
                  <img src={ProfilePlaceholder} alt="Profile" className="w-20 h-20 rounded-full" />
                  <div>
                    <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                      <User className="mr-2" size={20} />
                      Personal Information
                    </h2>
                    <p className="text-sm text-gray-600 mt-1">Customer Profile Details</p>
                  </div>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">First Name</label>
                    <p className="text-gray-900">{bankCustomerData.fullName.split(' ')[0]}</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Last Name</label>
                    <p className="text-gray-900">{bankCustomerData.fullName.split(' ').slice(1).join(' ')}</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Date of Birth</label>
                    <p className="text-gray-900 flex items-center">
                      <Calendar className="mr-1" size={16} />
                      {new Date(bankCustomerData.dateOfBirth).toLocaleDateString('en-US', { 
                        month: 'numeric', 
                        day: 'numeric', 
                        year: 'numeric' 
                      })}
                    </p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Nationality</label>
                    <p className="text-gray-900">{bankCustomerData.nationality}</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">National ID</label>
                    <p className="text-gray-900 font-mono">{bankCustomerData.nationalId}</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Email</label>
                    <p className="text-gray-900 flex items-center">
                      <Mail className="mr-1" size={16} />
                      {bankCustomerData.email}
                    </p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Phone</label>
                    <p className="text-gray-900 flex items-center">
                      <Phone className="mr-1" size={16} />
                      {bankCustomerData.phoneNumber}
                    </p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Marital Status</label>
                    <p className="text-gray-900">{bankCustomerData.maritalStatus}</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Occupation</label>
                    <p className="text-gray-900">{bankCustomerData.occupation}</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Monthly Income</label>
                    <p className="text-gray-900">{bankCustomerData.monthlyIncome.toLocaleString('en-US')} MAD</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Account Type</label>
                    <p className="text-gray-900">{bankCustomerData.accountType}</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Account Status</label>
                    <p className={`text-sm font-medium px-2 py-1 rounded-full inline-block ${
                      bankCustomerData.accountStatus === 'Active' 
                        ? 'bg-green-100 text-green-800' 
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {bankCustomerData.accountStatus}
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                  <MapPin className="mr-2" size={20} />
                  Address Information
                </h2>
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  <div className="space-y-3">
                    <div className="space-y-2">
                      <p className="text-gray-900 font-medium">{bankCustomerData.address.street}</p>
                      <p className="text-gray-900">{bankCustomerData.address.city}, {bankCustomerData.address.postalCode}</p>
                      <p className="text-gray-900">{bankCustomerData.address.country}</p>
                    </div>
                    <div className="pt-2 space-y-2">
                      <a 
                        href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${bankCustomerData.address.street}, ${bankCustomerData.address.city}, ${bankCustomerData.address.country}`)}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center text-blue-600 hover:text-blue-800 text-sm font-medium mr-4"
                      >
                        <MapPin className="mr-1" size={16} />
                        Google Maps
                      </a>
                      <a 
                        href={`https://www.openstreetmap.org/search?query=${encodeURIComponent(`${bankCustomerData.address.street}, ${bankCustomerData.address.city}, ${bankCustomerData.address.country}`)}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center text-green-600 hover:text-green-800 text-sm font-medium"
                      >
                        <MapPin className="mr-1" size={16} />
                        OpenStreetMap
                      </a>
                    </div>
                  </div>
                  
                  <div className="h-48 rounded-lg overflow-hidden border border-gray-200">
                    <iframe
                      src={`https://www.openstreetmap.org/export/embed.html?bbox=-7.6500,33.5731,-7.5600,33.6131&layer=mapnik&marker=33.5931,-7.6067`}
                      width="100%"
                      height="100%"
                      style={{ border: 0 }}
                      loading="lazy"
                      title="Customer Address Location"
                    />
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-6 flex items-center">
                  <User className="mr-2" size={20} />
                  Account Information
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Account Number</label>
                    <p className="text-gray-900 font-mono">{bankCustomerData.accountNumber}</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Account Opening Date</label>
                    <p className="text-gray-900">{new Date(bankCustomerData.accountOpeningDate).toLocaleDateString('en-US', { 
                      month: 'long', 
                      day: 'numeric', 
                      year: 'numeric' 
                    })}</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Credit Score</label>
                    <p className={`text-sm font-medium px-2 py-1 rounded-full inline-block ${
                      bankCustomerData.creditScore >= 700 
                        ? 'bg-green-100 text-green-800' 
                        : bankCustomerData.creditScore >= 600
                        ? 'bg-yellow-100 text-yellow-800'
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {bankCustomerData.creditScore}
                    </p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Total Balance</label>
                    <p className="text-gray-900 font-semibold">{bankCustomerData.totalBalance.toLocaleString('en-US')} MAD</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Available Credit</label>
                    <p className="text-gray-900">{bankCustomerData.availableCredit.toLocaleString('en-US')} MAD</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Risk Level</label>
                    <p className={`text-sm font-medium px-2 py-1 rounded-full inline-block ${
                      bankCustomerData.riskLevel === 'Low' 
                        ? 'bg-green-100 text-green-800' 
                        : bankCustomerData.riskLevel === 'Medium'
                        ? 'bg-yellow-100 text-yellow-800'
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {bankCustomerData.riskLevel}
                    </p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">KYC Status</label>
                    <p className={`text-sm font-medium px-2 py-1 rounded-full inline-block ${
                      bankCustomerData.kycStatus === 'Verified' 
                        ? 'bg-green-100 text-green-800' 
                        : 'bg-yellow-100 text-yellow-800'
                    }`}>
                      {bankCustomerData.kycStatus}
                    </p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Branch</label>
                    <p className="text-gray-900">{bankCustomerData.branch}</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-500">Relationship Manager</label>
                    <p className="text-gray-900">{bankCustomerData.relationshipManager}</p>
                  </div>
                </div>
              </div>


            </div>
          </div>
        )}

        {activeTab === 'documents' && (
          <div className="h-full overflow-y-auto p-6">
            <div className="max-w-4xl mx-auto">
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-6 flex items-center">
                  <FileText className="mr-2" size={20} />
                  Customer Documents
                </h2>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="space-y-3">
                    <h3 className="text-md font-medium text-gray-700">National ID - Front</h3>
                    <div className="border border-gray-200 rounded-lg overflow-hidden">
                      <img 
                        src={FrontIdImage} 
                        alt="ID Front" 
                        className="w-full h-64 object-cover"
                      />
                    </div>
                  </div>

                  <div className="space-y-3">
                    <h3 className="text-md font-medium text-gray-700">National ID - Back</h3>
                    <div className="border border-gray-200 rounded-lg overflow-hidden">
                      <img 
                        src={BackIdImage} 
                        alt="ID Back" 
                        className="w-full h-64 object-cover"
                      />
                    </div>
                  </div>
                </div>

                <div className="mt-8">
                  <h3 className="text-md font-medium text-gray-700 mb-4">Additional Documents</h3>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center space-x-3">
                        <FileText size={20} className="text-gray-500" />
                        <div>
                          <p className="text-sm font-medium text-gray-900">Proof of Address</p>
                          <p className="text-xs text-gray-500">Utility bill or bank statement</p>
                        </div>
                      </div>
                      <button className="text-blue-500 hover:text-blue-700 text-sm font-medium">
                        View
                      </button>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center space-x-3">
                        <FileText size={20} className="text-gray-500" />
                        <div>
                          <p className="text-sm font-medium text-gray-900">Income Certificate</p>
                          <p className="text-xs text-gray-500">Employment verification</p>
                        </div>
                      </div>
                      <button className="text-blue-500 hover:text-blue-700 text-sm font-medium">
                        View
                      </button>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center space-x-3">
                        <FileText size={20} className="text-gray-500" />
                        <div>
                          <p className="text-sm font-medium text-gray-900">Bank Statements</p>
                          <p className="text-xs text-gray-500">Last 3 months</p>
                        </div>
                      </div>
                      <button className="text-blue-500 hover:text-blue-700 text-sm font-medium">
                        View
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'chat' && customer && (
          <ChatView 
            conversation={customer.conversation} 
            onBack={() => {}} 
          />
        )}
        </div>
      </div>
    </div>
  )
}

export default BankCustomerView
