import { BaseApiController } from './BaseApiController.js';

/**
 * Documentation Service Controller
 * Handles all docs-related API calls
 */
export class DocsController extends BaseApiController {
  constructor() {
    super('http://localhost:8082/api/docs');
  }

  /**
   * Check service health
   */
  async checkHealth() {
    return this.get('/health');
  }
}