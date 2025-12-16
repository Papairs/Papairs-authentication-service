export const API_BASE_URL = process.env.VUE_APP_ORCHESTRATION_SERVICE_URL || 'http://localhost:8080';

export const getWebSocketUrl = () => {
  return API_BASE_URL + '/ws/doc';
};

export const getCollaborationWebSocketUrl = () => {
  // Y.js collaboration service WebSocket URL
  // Connect directly to collaboration service (bypass gateway for WebSocket)
  if (process.env.NODE_ENV === 'development') {
    return 'ws://localhost:8083';
  }
  // In production, use the gateway
  const baseUrl = API_BASE_URL.replace('http://', 'ws://').replace('https://', 'wss://');
  return baseUrl + '/collaboration';
};