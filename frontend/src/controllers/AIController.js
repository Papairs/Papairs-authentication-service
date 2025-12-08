import { BaseApiController } from './BaseApiController.js';
import { API_BASE_URL } from '@/config';

/**
 * AI Service Controller
 * Handles all AI-related API calls through the orchestration gateway
 */
export class AIController extends BaseApiController {
  constructor() {
    super(API_BASE_URL);
    this.servicePath = '/api/ai';
  }

  /**
   * Get autocompletion suggestions from AI
   * @param {Object} requestData - { userInput }
   */
  async getAutocompletion(requestData) {
    return this.post(`${this.servicePath}/autocomplete`, requestData);
  }

  /**
   * Generate flashcards from content using AI
   * @param {Object} requestData - { content, numberOfCards }
   */
  async generateFlashcards(requestData) {
    return this.post(`${this.servicePath}/generate-flashcards`, requestData);
  }

  /**
   * Check if AI service is responding
   */
  async checkHealth() {
    try {
      const response = await this.get(`${this.servicePath}/health`);
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