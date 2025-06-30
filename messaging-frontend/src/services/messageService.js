import axios from 'axios';

const API_URL = 'http://localhost:8080/api/messages';

export const messageService = {
  getMessages: async (conversationId, page = 0, size = 10) => {
    try {
      const response = await axios.get(`${API_URL}/conversation/${conversationId}`, {
        params: {
          page,
          size
        }
      });
      return {
        messages: response.data.content,
        totalPages: response.data.totalPages,
        totalElements: response.data.totalElements,
        isLast: response.data.last,
        isFirst: response.data.first,
        currentPage: response.data.number
      };
    } catch (error) {
      console.error('Error fetching messages:', error);
      throw error;
    }
  },
  sendMessage: async (messageData) => {
    try {
      const response = await axios.post(`${API_URL}`, messageData);
      return response.data;
    } catch (error) {
      console.error('Error sending message:', error);
      throw error;
    }
  },
  editMessage: async (messageId, content) => {
    try {
      const response = await axios.patch(`${API_URL}/${messageId}`, { content });
      return response.data;
    } catch (error) {
      console.error('Error editing message:', error);
      throw error;
    }
  },
  replyTo: async (messageId, messageData) => {
    try {
      const response = await axios.post(`${API_URL}/reply/${messageId}`, messageData);
      return response.data;
    } catch (error) {
      console.error('Error replying to message:', error);
      throw error;
    }
  }
}; 