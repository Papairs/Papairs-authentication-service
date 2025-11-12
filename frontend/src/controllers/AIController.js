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
   * Note: AI service doesn't have a dedicated health endpoint, 
   * so we'll test with a simple autocomplete request
   */
  async checkHealth() {
    try {
      await this.post('/autocomplete', { userInput: 'test' });
      return {
        success: true,
        data: { status: 'healthy', message: 'AI service is responding' },
        status: 200
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