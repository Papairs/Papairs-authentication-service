export const API_BASE_URL = process.env.VUE_APP_ORCHESTRATION_SERVICE_URL || 'http://localhost:8080';

export const getWebSocketUrl = () => {
  return API_BASE_URL.replace(/^http/, 'ws') + '/ws/doc';
};