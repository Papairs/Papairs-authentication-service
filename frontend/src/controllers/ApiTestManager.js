/**
 * API Test Manager
 * Manages all API endpoint tests for the admin interface
 */
export class ApiTestManager {
  constructor() {
    this.testResults = {};
    this.isLoading = {};
  }

  /**
   * Set loading state for a test
   */
  setLoading(testKey, isLoading) {
    this.isLoading[testKey] = isLoading;
  }

  /**
   * Check if test is loading
   */
  isTestLoading(testKey) {
    return this.isLoading[testKey] || false;
  }

  /**
   * Store test result
   */
  setResult(testKey, result) {
    this.testResults[testKey] = {
      ...result,
      timestamp: new Date().toISOString()
    };
  }

  /**
   * Get test result
   */
  getResult(testKey) {
    return this.testResults[testKey] || null;
  }

  /**
   * Clear test result
   */
  clearResult(testKey) {
    delete this.testResults[testKey];
  }

  /**
   * Clear all results
   */
  clearAllResults() {
    this.testResults = {};
    this.isLoading = {};
  }

  /**
   * Execute a test with error handling and loading state
   */
  async executeTest(testKey, testFunction, ...args) {
    this.setLoading(testKey, true);
    this.clearResult(testKey);

    try {
      const result = await testFunction(...args);
      this.setResult(testKey, result);
      return result;
    } catch (error) {
      const errorResult = {
        success: false,
        error: error.message || 'Test failed',
        status: error.status || 500
      };
      this.setResult(testKey, errorResult);
      return errorResult;
    } finally {
      this.setLoading(testKey, false);
    }
  }

  /**
   * Format result for display
   */
  formatResult(result) {
    if (!result) return '';
    
    return {
      status: result.success ? 'success' : 'error',
      message: result.success ? 'Success' : result.error,
      data: result.data || null,
      httpStatus: result.status || 'Unknown',
      timestamp: result.timestamp || new Date().toISOString()
    };
  }

  /**
   * Get summary of all test results
   */
  getTestSummary() {
    const results = Object.values(this.testResults);
    const total = results.length;
    const successful = results.filter(r => r.success).length;
    const failed = total - successful;

    return {
      total,
      successful,
      failed,
      successRate: total > 0 ? Math.round((successful / total) * 100) : 0
    };
  }
}