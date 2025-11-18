# Papairs Drive - MVP Overview

## Overview
A Google Drive-like interface for managing folders and documents in the Papairs application.

## What Was Created

### 1. Main View Component
**File:** `frontend/src/views/DriveView.vue`
- Google Drive-inspired layout with header, breadcrumbs, and grid view
- Displays folders and documents in a responsive grid
- Support for creating, renaming, and deleting folders and documents
- Folder navigation with breadcrumb trail
- Dark mode support

### 2. Card Components

#### FolderCard (`frontend/src/components/FolderCard.vue`)
- Visual representation of folders with folder icon
- Hover menu with rename and delete options
- Click to navigate into the folder
- Shows creation date

#### DocumentCard (`frontend/src/components/DocumentCard.vue`)
- Visual representation of documents with document icon
- Hover menu with rename and delete options
- Click to open the document in the editor
- Shows last updated date

### 3. Modal Components

#### CreateFolderModal (`frontend/src/components/CreateFolderModal.vue`)
- Modal dialog for creating new folders
- Input validation
- Error handling

#### CreateDocumentModal (`frontend/src/components/CreateDocumentModal.vue`)
- Modal dialog for creating new documents
- Automatically navigates to the new document after creation
- Error handling

#### RenameFolderModal (`frontend/src/components/RenameFolderModal.vue`)
- Modal dialog for renaming existing folders
- Pre-filled with current folder name

#### RenameDocumentModal (`frontend/src/components/RenameDocumentModal.vue`)
- Modal dialog for renaming existing documents
- Pre-filled with current document title

### 4. API Service
**File:** `frontend/src/utils/driveService.js`

A comprehensive service layer that connects to your backend endpoints:

**Folder Operations:**
- `getRootFolders()` - Get all root-level folders
- `getAllFolders()` - Get all user folders
- `getFolder(folderId)` - Get specific folder details
- `getChildFolders(folderId)` - Get subfolders
- `getFolderPath(folderId)` - Get breadcrumb path
- `createFolder(name, parentFolderId)` - Create new folder
- `renameFolder(folderId, newName)` - Rename folder
- `deleteFolder(folderId, recursive)` - Delete folder (with option to delete contents)
- `moveFolder(folderId, parentFolderId)` - Move folder to different parent

**Document Operations:**
- `getAllDocuments()` - Get all user documents
- `getDocument(pageId)` - Get specific document
- `createDocument(title, folderId)` - Create new document
- `renameDocument(pageId, newTitle)` - Rename document
- `deleteDocument(pageId)` - Delete document
- `moveDocument(pageId, folderId)` - Move document to folder
- `updateDocument(pageId, content)` - Update document content

### 5. Router Updates
**File:** `frontend/src/router/index.js`

Added new routes:
- `/drive` - Main drive view (root level)
- `/drive/:folderId` - Folder-specific view

### 6. Navigation Updates
**File:** `frontend/src/views/HomeView.vue`

Added "Drive" link to the main navigation menu.

## How to Use

### Access the Drive
1. Navigate to `http://localhost:3000/drive`
2. You'll see the main drive interface

### Features

#### Create a New Folder
1. Click the "New Folder" button
2. Enter folder name in the modal
3. Click "Create"

#### Create a New Document
1. Click the "New Document" button
2. Enter document title in the modal
3. Click "Create" - you'll be redirected to the document editor

#### Navigate into Folders
- Click on any folder card to navigate into it
- Use breadcrumbs at the top to navigate back to parent folders
- Click "My Drive" to return to the root

#### Rename Items
1. Hover over a folder or document card
2. Click the three-dot menu button
3. Select "Rename"
4. Enter new name and confirm

#### Delete Items
1. Hover over a folder or document card
2. Click the three-dot menu button
3. Select "Delete"
4. Confirm deletion
5. **Note:** Deleting folders will delete all contents inside

#### Open Documents
- Click on any document card to open it in the editor

## Backend Integration

The frontend connects to these backend endpoints:

### Folders
- `GET /api/docs/folders/roots` - Root folders
- `GET /api/docs/folders` - All folders
- `GET /api/docs/folders/{folderId}` - Folder details
- `GET /api/docs/folders/{folderId}/children` - Child folders
- `GET /api/docs/folders/{folderId}/path` - Breadcrumb path
- `POST /api/docs/folders` - Create folder
- `PATCH /api/docs/folders/{folderId}` - Rename folder
- `DELETE /api/docs/folders/{folderId}` - Delete folder

### Documents
- `GET /api/docs/pages` - All documents
- `GET /api/docs/pages/{pageId}` - Document details
- `POST /api/docs/pages` - Create document
- `PATCH /api/docs/pages/{pageId}` - Rename document
- `DELETE /api/docs/pages/{pageId}` - Delete document

## User Authentication

The service uses `auth.getUserIdHeader()` to attach the user ID to all requests via the `X-User-Id` header. This makes it easy to add proper authentication later by simply updating the auth utility.

## Future Enhancements (Not in MVP)

- Sharing functionality
- Permissions management
- Search functionality
- File upload
- Move operations (drag & drop)
- List view (in addition to grid view)
- Sorting and filtering options
- Starred/favorite items
- Recent items view
- Trash/recycle bin

## Styling

The interface uses your existing Tailwind CSS theme with:
- Light/dark mode support
- Accent color (#FF7700)
- Responsive grid layout
- Hover effects and transitions
- Consistent with existing Papairs design

## Starting the Application

Make sure your backend services are running:
```bash
# From the root directory
docker-compose up
```

Then start the frontend:
```bash
cd frontend
npm run serve
```

Navigate to `http://localhost:3000/drive` to see your new Drive interface!
