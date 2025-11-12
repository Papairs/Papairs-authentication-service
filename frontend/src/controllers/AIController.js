import { BaseApiController } from './BaseApiController.js';

/**
 * AI Service Controller
 * Handles all AI-related API calls
 */
export class AIController extends BaseApiController {
  constructor() {
    super('http://localhost:3001');
  }

  /**
   * Get autocompletion suggestions from AI
   * @param {Object} requestData - { userInput }
   */
  async getAutocompletion(requestData) {
    return this.post('/autocomplete', requestData);
  }

  /**
   * Check if AI service is responding
   */
  async checkHealth() {
    try {
      const response = await this.get('/health');
      return {
        success: true,
        data: response.data,
        status: response.status
      };
    } catch (error) {
      return {
        success: false,
        error: `AI service health check failed: ${error}`,
        status: 500
      };
    }
  }
}