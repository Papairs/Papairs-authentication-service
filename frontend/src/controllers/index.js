// Controller exports
export { BaseApiController } from './BaseApiController.js';
export { AuthController } from './AuthController.js';
export { DocsController } from './DocsController.js';
export { PageController } from './PageController.js';
export { FolderController } from './FolderController.js';

// Import controller classes
import { AuthController } from './AuthController.js';
import { DocsController } from './DocsController.js';  
import { PageController } from './PageController.js';
import { FolderController } from './FolderController.js';

// Controller instances
export const authController = new AuthController();
export const docsController = new DocsController();
export const pageController = new PageController();
export const folderController = new FolderController();