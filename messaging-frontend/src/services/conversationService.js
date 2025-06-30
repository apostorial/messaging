import axios from 'axios';

const API_URL = 'http://localhost:8080/api/conversations';

export const conversationService = {
  getConversations: async (page = 0, size = 10) => {
    try {
      const response = await axios.get(`${API_URL}`, {
        params: {
          page,
          size
        }
      });
      return {
        conversations: response.data.content,
        totalPages: response.data.totalPages,
        totalElements: response.data.totalElements,
        isLast: response.data.last,
        isFirst: response.data.first,
        currentPage: response.data.number
      };
    } catch (error) {
      console.error('Error fetching conversations:', error);
      throw error;
    }
  }
}; 