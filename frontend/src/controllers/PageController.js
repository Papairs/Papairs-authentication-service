import { BaseApiController } from './BaseApiController.js';

/**
 * Page Management Controller
 * Handles all page-related API calls
 */
export class PageController extends BaseApiController {
  constructor() {
    super('http://localhost:8082/api/docs');
  }

  /**
   * Get all pages user has access to
   */
  async getAllPages() {
    return this.get('/pages');
  }

  /**
   * Create a new page
   * @param {Object} pageData - { title, folderId? }
   */
  async createPage(pageData) {
    return this.post('/pages', pageData);
  }

  /**
   * Get page by ID
   * @param {string} pageId - The page ID
   */
  async getPage(pageId) {
    return this.get(`/pages/${pageId}`);
  }

  /**
   * Update page content
   * @param {string} pageId - The page ID
   * @param {Object} updateData - { content }
   */
  async updatePage(pageId, updateData) {
    return this.put(`/pages/${pageId}`, updateData);
  }

  /**
   * Rename a page
   * @param {string} pageId - The page ID
   * @param {Object} renameData - { newTitle }
   */
  async renamePage(pageId, renameData) {
    return this.patch(`/pages/${pageId}`, renameData);
  }

  /**
   * Move page to different folder
   * @param {string} pageId - The page ID
   * @param {Object} moveData - { folderId }
   */
  async movePage(pageId, moveData) {
    return this.patch(`/pages/${pageId}/move`, moveData);
  }

  /**
   * Delete a page
   * @param {string} pageId - The page ID
   */
  async deletePage(pageId) {
    return this.delete(`/pages/${pageId}`);
  }
}