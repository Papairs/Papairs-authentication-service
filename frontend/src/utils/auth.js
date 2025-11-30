/**
 * Authentication utility functions
 * Provides easy access to stored user data and tokens
 */

export const auth = {
  /**
   * Get the stored authentication token
   * @returns {string|null} The session token or null if not found
   */
  getToken() {
    return localStorage.getItem('papairs_token')
  },

  /**
   * Get the stored user data
   * @returns {object|null} The user object or null if not found
   */
  getUser() {
    try {
      const userData = localStorage.getItem('papairs_user')
      return userData ? JSON.parse(userData) : null
    } catch (error) {
      console.error('Error parsing user data from localStorage:', error)
      return null
    }
  },

  /**
   * Get the user ID specifically
   * @returns {string|null} The user ID or null if not found
   */
  getUserId() {
    const user = this.getUser()
    return user?.id || null
  },

  /**
   * Get user email
   * @returns {string|null} The user email or null if not found
   */
  getUserEmail() {
    const user = this.getUser()
    return user?.email || null
  },

  /**
   * Check if user is authenticated (has valid token)
   * @returns {boolean} True if user has a token stored
   */
  isAuthenticated() {
    return !!this.getToken()
  },

  /**
   * Check if user is active and verified
   * @returns {boolean} True if user is active and email verified
   */
  isUserActive() {
    const user = this.getUser()
    return user?.isActive && user?.emailVerified
  },

  /**
   * Get authorization header for API requests
   * @returns {object} Authorization header object or empty object
   */
  getAuthHeader() {
    const token = this.getToken()
    return token ? { 'Authorization': `Bearer ${token}` } : {}
  },

  /**
   * Get all auth headers for API requests (Authorization + X-User-Id)
   * @returns {Promise<object>} Combined auth headers object
   */
  async getAuthHeaders() {
    const token = this.getToken()
    const userId = this.getUserId()
    
    const headers = {}
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    if (userId) {
      headers['X-User-Id'] = userId
    }
    
    return headers
  },

  /**
   * Get user ID header for API requests (for services that need X-User-Id)
   * @returns {object} User ID header object or empty object
   */
  getUserIdHeader() {
    const userId = this.getUserId()
    return userId ? { 'X-User-Id': userId } : {}
  },

  /**
   * Clear all authentication data (logout)
   */
  logout() {
    localStorage.removeItem('papairs_token')
    localStorage.removeItem('papairs_user')
  },

  /**
   * Store user data (used by login)
   * @param {string} token - Session token
   * @param {object} user - User data object
   */
  setAuthData(token, user) {
    localStorage.setItem('papairs_token', token)
    localStorage.setItem('papairs_user', JSON.stringify({
      id: user.id,
      email: user.email,
      emailVerified: user.emailVerified,
      isActive: user.isActive
    }))
  }
}

export default auth