<template>
  <div class="tiptap-editor-container flex flex-col h-full">
    <!-- Formatting Toolbar -->
    <div class="toolbar flex items-center gap-1 p-2 border-b border-gray-200 bg-white flex-shrink-0 sticky top-0 z-10">
      <!-- Text Formatting -->
      <button
        @click="editor?.chain().focus().toggleBold().run()"
        :class="{ 'is-active': editor?.isActive('bold') }"
        class="toolbar-button"
        title="Bold"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M15.6 10.79c.97-.67 1.65-1.77 1.65-2.79 0-2.26-1.75-4-4-4H7v14h7.04c2.09 0 3.71-1.7 3.71-3.79 0-1.52-.86-2.82-2.15-3.42zM10 6.5h3c.83 0 1.5.67 1.5 1.5s-.67 1.5-1.5 1.5h-3v-3zm3.5 9H10v-3h3.5c.83 0 1.5.67 1.5 1.5s-.67 1.5-1.5 1.5z"/>
        </svg>
      </button>

      <button
        @click="editor?.chain().focus().toggleItalic().run()"
        :class="{ 'is-active': editor?.isActive('italic') }"
        class="toolbar-button"
        title="Italic"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M10 4v3h2.21l-3.42 8H6v3h8v-3h-2.21l3.42-8H18V4z"/>
        </svg>
      </button>

      <button
        @click="editor?.chain().focus().toggleUnderline().run()"
        :class="{ 'is-active': editor?.isActive('underline') }"
        class="toolbar-button"
        title="Underline"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 17c3.31 0 6-2.69 6-6V3h-2.5v8c0 1.93-1.57 3.5-3.5 3.5S8.5 12.93 8.5 11V3H6v8c0 3.31 2.69 6 6 6zm-7 2v2h14v-2H5z"/>
        </svg>
      </button>

      <button
        @click="editor?.chain().focus().toggleCode().run()"
        :class="{ 'is-active': editor?.isActive('code') }"
        class="toolbar-button"
        title="Inline Code"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M8.7 15.9L4.8 12l3.9-3.9c.39-.39.39-1.01 0-1.4-.39-.39-1.01-.39-1.4 0l-4.59 4.59c-.39.39-.39 1.02 0 1.41L7.3 17.3c.39.39 1.02.39 1.41 0 .38-.39.38-1.01-.01-1.4zm6.6 0l3.9-3.9-3.9-3.9c-.39-.39-.39-1.01 0-1.4.39-.39 1.01-.39 1.4 0l4.59 4.59c.39.39.39 1.02 0 1.41L16.7 17.3c-.39.39-1.02.39-1.41 0-.38-.39-.38-1.01.01-1.4z"/>
        </svg>
      </button>

      <!-- Separator -->
      <div class="w-px h-6 bg-gray-300 mx-2"></div>

      <!-- Headings -->
      <select 
        @change="setHeading($event)"
        class="toolbar-select"
        :value="getActiveHeading()"
      >
        <option value="">Normal</option>
        <option value="1">Heading 1</option>
        <option value="2">Heading 2</option>
        <option value="3">Heading 3</option>
        <option value="4">Heading 4</option>
        <option value="5">Heading 5</option>
        <option value="6">Heading 6</option>
      </select>

      <!-- Separator -->
      <div class="w-px h-6 bg-gray-300 mx-2"></div>

      <!-- Lists -->
      <button
        @click="editor?.chain().focus().toggleBulletList().run()"
        :class="{ 'is-active': editor?.isActive('bulletList') }"
        class="toolbar-button"
        title="Bullet List"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M4 10.5c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5zm0-6c-.83 0-1.5.67-1.5 1.5S3.17 7.5 4 7.5 5.5 6.83 5.5 6 4.83 4.5 4 4.5zm0 12c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5zM7 19h14v-2H7v2zm0-6h14v-2H7v2zm0-8v2h14V5H7z"/>
        </svg>
      </button>

      <button
        @click="editor?.chain().focus().toggleOrderedList().run()"
        :class="{ 'is-active': editor?.isActive('orderedList') }"
        class="toolbar-button"
        title="Numbered List"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M2 17h2v.5H3v1h1v.5H2v1h3v-4H2v1zm1-9h1V4H2v1h1v3zm-1 3h1.8L2 13.1v.9h3v-1H3.2L5 10.9V10H2v1zm5-6v2h14V5H7zm0 14h14v-2H7v2zm0-6h14v-2H7v2z"/>
        </svg>
      </button>

      <!-- Separator -->
      <div class="w-px h-6 bg-gray-300 mx-2"></div>

      <!-- Code Block -->
      <button
        @click="editor?.chain().focus().toggleCodeBlock().run()"
        :class="{ 'is-active': editor?.isActive('codeBlock') }"
        class="toolbar-button"
        title="Code Block"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M14.6 16.6l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4zm-5.2 0L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4z"/>
        </svg>
      </button>

      <!-- Separator -->
      <div class="w-px h-6 bg-gray-300 mx-2"></div>

      <!-- Undo/Redo -->
      <button
        @click="editor?.chain().focus().undo().run()"
        :disabled="!editor?.can().undo()"
        class="toolbar-button"
        title="Undo"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z"/>
        </svg>
      </button>

      <button
        @click="editor?.chain().focus().redo().run()"
        :disabled="!editor?.can().redo()"
        class="toolbar-button"
        title="Redo"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M18.4 10.6C16.55 8.99 14.15 8 11.5 8c-4.65 0-8.58 3.03-9.96 7.22L3.9 16c1.05-3.19 4.05-5.5 7.6-5.5 1.95 0 3.73.72 5.12 1.88L13 16h9V7l-3.6 3.6z"/>
        </svg>
      </button>

      <!-- Auto-save indicator -->
      <div class="ml-auto flex items-center gap-2 text-sm text-gray-500">
        <div v-if="isSaving" class="flex items-center gap-1">
          <div class="animate-spin w-3 h-3 border border-gray-400 border-t-transparent rounded-full"></div>
          <span>Saving...</span>
        </div>
        <div v-else-if="lastSaved" class="text-green-600">
          <span>Saved {{ formatTime(lastSaved) }}</span>
        </div>
      </div>
    </div>

    <!-- Editor Content -->
    <div class="editor-content-wrapper flex-1">
      <editor-content 
        :editor="editor" 
        class="px-12 py-12 prose prose-lg max-w-none focus:outline-none"
      />
    </div>
  </div>
</template>

<script>
import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import { onBeforeUnmount, ref, watch, nextTick } from 'vue'
import './TiptapEditor.css'

export default {
  name: 'TiptapEditor',
  components: {
    EditorContent,
  },
  props: {
    modelValue: {
      type: String,
      default: '',
    },
    placeholder: {
      type: String,
      default: 'Start writing your document...',
    },
    autosaveDelay: {
      type: Number,
      default: 5000, // 5 seconds
    }
  },
  emits: ['update:modelValue', 'autosave', 'ready', 'content-change'],
  setup(props, { emit }) {
    const editor = ref(null)
    const autosaveTimer = ref(null)
    const isSaving = ref(false)
    const lastSaved = ref(null)
    const isInitialized = ref(false)
    const lastContent = ref('')

    // Initialize Tiptap editor
    const initEditor = () => {
      editor.value = new Editor({
        extensions: [
          StarterKit.configure({
            history: {
              depth: 100,
            },
          }),
          Underline,
        ],
        content: props.modelValue,
        onUpdate: ({ editor }) => {
          const html = editor.getHTML()
          
          // Only emit update if content actually changed and editor is initialized
          if (isInitialized.value && html !== lastContent.value) {
            lastContent.value = html
            emit('update:modelValue', html)
            emit('content-change', html)
            
            // Reset autosave timer - only start after user stops typing
            if (autosaveTimer.value) {
              clearTimeout(autosaveTimer.value)
            }
            scheduleAutosave()
          }
        },
        onCreate: ({ editor }) => {
          nextTick(() => {
            lastContent.value = editor.getHTML()
            isInitialized.value = true
            emit('ready', editor)
          })
        },
        editorProps: {
          attributes: {
            class: 'prose prose-lg max-w-none focus:outline-none min-h-full',
            spellcheck: 'false',
          },
        },
      })
    }

    // Schedule autosave - only triggers after user stops typing
    const scheduleAutosave = () => {
      autosaveTimer.value = setTimeout(() => {
        triggerAutosave()
      }, props.autosaveDelay)
    }

    // Trigger autosave
    const triggerAutosave = async () => {
      if (!editor.value) return
      
      isSaving.value = true
      try {
        const html = editor.value.getHTML()
        await emit('autosave', html)
        lastSaved.value = new Date()
      } catch (error) {
        console.error('Autosave failed:', error)
      } finally {
        isSaving.value = false
      }
    }

    // Handle heading selection
    const setHeading = (event) => {
      if (!editor.value) return
      
      const level = event.target.value
      if (level) {
        editor.value.chain().focus().toggleHeading({ level: parseInt(level) }).run()
      } else {
        editor.value.chain().focus().setParagraph().run()
      }
    }

    // Get active heading level
    const getActiveHeading = () => {
      if (!editor.value) return ''
      
      for (let level = 1; level <= 6; level++) {
        if (editor.value.isActive('heading', { level })) {
          return level.toString()
        }
      }
      return ''
    }

    // Format time for last saved indicator
    const formatTime = (date) => {
      const now = new Date()
      const diff = Math.floor((now - date) / 1000)
      
      if (diff < 60) return 'just now'
      if (diff < 3600) return `${Math.floor(diff / 60)}m ago`
      if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`
      return date.toLocaleDateString()
    }

    // Watch for external content changes (e.g., from WebSocket)
    watch(() => props.modelValue, (newValue) => {
      if (editor.value && isInitialized.value && newValue !== lastContent.value) {
        const currentContent = editor.value.getHTML()
        if (newValue !== currentContent) {
          // Temporarily disable change detection
          isInitialized.value = false
          editor.value.commands.setContent(newValue, false)
          lastContent.value = newValue
          nextTick(() => {
            isInitialized.value = true
          })
        }
      }
    })

    // Initialize editor on mount
    initEditor()

    // Cleanup on unmount
    onBeforeUnmount(() => {
      if (autosaveTimer.value) {
        clearTimeout(autosaveTimer.value)
      }
      if (editor.value) {
        editor.value.destroy()
      }
    })

    return {
      editor,
      isSaving,
      lastSaved,
      setHeading,
      getActiveHeading,
      formatTime,
    }
  },
}
</script>