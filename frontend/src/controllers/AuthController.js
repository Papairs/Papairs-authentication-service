import { BaseApiController } from './BaseApiController.js';

/**
 * Authentication Service Controller
 * Handles all authentication-related API calls
 */
export class AuthController extends BaseApiController {
  constructor() {
    super('http://localhost:8080/api/auth');
  }

  /**
   * Check service health
   */
  async checkHealth() {
    return this.get('/health');
  }

  /**
   * Login user
   * @param {Object} credentials - { email, password }
   */
  async login(credentials) {
    const result = await this.post('/login', credentials);
    
    // Store auth data on successful login
    if (result.success && result.data.sessionToken) {
      localStorage.setItem('papairs_token', result.data.sessionToken);
      if (result.data.user) {
        localStorage.setItem('papairs_user', JSON.stringify({
          id: result.data.user.id,
          email: result.data.user.email,
          emailVerified: result.data.user.emailVerified || false,
          isActive: result.data.user.isActive || true
        }));
      }
    }
    
    return result;
  }

  /**
   * Register new user
   * @param {Object} userDetails - { email, password }
   */
  async register(userDetails) {
    return this.post('/register', userDetails);
  }

  /**
   * Logout current user
   */
  async logout() {
    const result = await this.post('/logout');
    
    // Clear stored auth data
    localStorage.removeItem('papairs_token');
    localStorage.removeItem('papairs_user');
    
    return result;
  }

  /**
   * Validate current session token
   */
  async validateToken() {
    return this.post('/validate');
  }

  /**
   * Change user password
   * @param {Object} passwordData - { currentPassword, newPassword }
   */
  async changePassword(passwordData) {
    return this.post('/change-password', passwordData);
  }

  /**
   * Check if user is logged in
   */
  isLoggedIn() {
    return !!localStorage.getItem('papairs_token');
  }

  /**
   * Get current user ID
   */
  getCurrentUserId() {
    try {
      const userData = localStorage.getItem('papairs_user');
      const user = userData ? JSON.parse(userData) : null;
      return user?.id || null;
    } catch (error) {
      console.error('Error parsing user data from localStorage:', error);
      return null;
    }
  }

  /**
   * Clear authentication data
   */
  clearAuthData() {
    localStorage.removeItem('papairs_token');
    localStorage.removeItem('papairs_user');
  }
}