import { BaseApiController } from './BaseApiController.js';
import { API_BASE_URL } from "@/config";

/**
 * Documentation Service Controller
 * Handles all docs-related API calls
 */
export class DocsController extends BaseApiController {
  constructor() {
    super(API_BASE_URL);
    this.servicePath = '/api/docs';
  }

  /**
   * Check service health
   */
  async checkHealth() {
    return this.get('/actuator/health/services/docsService');
  }
}