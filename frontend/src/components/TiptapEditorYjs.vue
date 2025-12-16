<template>
  <div v-if="editor" class="tiptap-editor-container flex flex-col h-full">
    <!-- Formatting Toolbar -->
      <EditorToolbar
        :editor="editor"
        :active-heading="activeHeading"
        :is-active="isActive"
        :can-undo="canUndo"
        :can-redo="canRedo"
        :current-text-color="currentTextColor"
        :current-font-size="currentFontSize"
        @set-heading="setHeading"
        @toggle-bold="toggleBold"
        @toggle-italic="toggleItalic"
        @toggle-underline="toggleUnderline"
        @toggle-strike="toggleStrike"
        @toggle-code="toggleCode"
        @toggle-code-block="toggleCodeBlock"
        @toggle-bullet-list="toggleBulletList"
        @toggle-ordered-list="toggleOrderedList"
        @set-text-align="setTextAlign"
        @undo="undo"
        @redo="redo"
        @set-font-family="setFontFamily"
        @set-font-size="setFontSize"
        @increase-font-size="increaseFontSize"
        @decrease-font-size="decreaseFontSize"
        @set-text-color="setTextColor"
        class="min-w-[800px] flex justify-center"
      />
    <!-- Editor Content with centered max-width wrapper -->
    <div class="flex-1 flex justify-center w-full overflow-auto">
      <EditorContentArea
        :editor="editor"
        :placeholder="placeholder"
        ref="editorContentArea"
        class="w-full max-w-[800px] min-w-[800px] min-h-full h-fit"
      />
    </div>
  </div>
  <div v-else class="flex items-center justify-center h-full">
    <div class="text-content-primary">Loading editor...</div>
  </div>
</template>

<script>
import { Editor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import TextStyle from '@tiptap/extension-text-style'
import FontFamily from '@tiptap/extension-font-family'
import { Color } from '@tiptap/extension-color'
import { Extension } from '@tiptap/core'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { Decoration, DecorationSet } from '@tiptap/pm/view'
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCursor from '@tiptap/extension-collaboration-cursor'
import { onBeforeUnmount, ref, computed } from 'vue'
import EditorToolbar from './EditorToolbar.vue'
import EditorContentArea from './EditorContent.vue'
import './TiptapEditor.css'
import { API_BASE_URL } from '@/config'
import auth from '@/utils/auth'
import { useAssistedWriting } from '@/composables/useAssistedWriting'

export default {
  name: 'TiptapEditorYjs',
  components: {
    EditorToolbar,
    EditorContentArea,
  },
  props: {
    ydoc: {
      type: Object,
      required: true
    },
    provider: {
      type: Object,
      required: true
    },
    placeholder: {
      type: String,
      default: 'Start writing your document...',
    },
    user: {
      type: Object,
      required: true
    },
    initialContent: {
      type: String,
      default: null
    },
    selectedFiles: {
      type: Array,
      default: () => []
    }
  },
  emits: ['ready'],
  setup(props, { emit }) {
    // Constants
    const DEFAULT_TEXT_COLOR = '#000000'
    const DEFAULT_FONT_SIZE = '12pt'
    const FONT_SIZES = ['8pt', '9pt', '10pt', '11pt', '12pt', '14pt', '18pt', '24pt', '30pt', '36pt', '48pt', '60pt', '72pt', '96pt']
    
    // Assisted Writing
    const { isAssistedWritingEnabled } = useAssistedWriting()
    
    // State
    const editor = ref(null)
    const currentTextColor = ref(DEFAULT_TEXT_COLOR)
    const currentFontSize = ref(DEFAULT_FONT_SIZE)
    const aiSuggestion = ref(null)
    const isLoadingAI = ref(false)
    const lastLocalText = ref('')
    let aiTimer = null

    // AI Autocomplete Extension
    const AIAutocomplete = Extension.create({
      name: 'aiAutocomplete',

      addProseMirrorPlugins() {
        return [
          new Plugin({
            key: new PluginKey('aiAutocomplete'),
            state: {
              init() {
                return DecorationSet.empty
              },
              apply(tr, oldState) {
                // If there's a suggestion and decoration metadata
                if (tr.getMeta('addAISuggestion')) {
                  const { pos, suggestion } = tr.getMeta('addAISuggestion')
                  const decoration = Decoration.widget(pos, () => {
                    const span = document.createElement('span')
                    span.className = 'ai-suggestion'
                    span.style.color = '#FF7A00'
                    span.style.opacity = '0.6'
                    span.style.fontStyle = 'italic'
                    span.textContent = suggestion
                    return span
                  })
                  return DecorationSet.create(tr.doc, [decoration])
                }

                // Clear suggestion
                if (tr.getMeta('clearAISuggestion')) {
                  return DecorationSet.empty
                }

                // Map decorations through document changes
                return oldState.map(tr.mapping, tr.doc)
              }
            },
            props: {
              decorations(state) {
                return this.getState(state)
              },
              handleKeyDown(view, event) {
                const { state } = view
                const decorations = this.getState(state)
                
                // If Tab is pressed and there's a suggestion
                if (event.key === 'Tab' && decorations.find().length > 0) {
                  event.preventDefault()
                  
                  const suggestion = aiSuggestion.value
                  if (suggestion) {
                    const tr = state.tr
                    // Insert the suggestion at current position
                    tr.insertText(suggestion, tr.selection.from)
                    tr.setMeta('clearAISuggestion', true)
                    view.dispatch(tr)
                    
                    aiSuggestion.value = null
                  }
                  return true
                }

                // Clear suggestion on any other key
                if (decorations.find().length > 0 && event.key !== 'Tab') {
                  const tr = state.tr.setMeta('clearAISuggestion', true)
                  view.dispatch(tr)
                  aiSuggestion.value = null
                }

                return false
              }
            }
          })
        ]
      }
    })

    // Custom font size extension
    const FontSize = Extension.create({
      name: 'fontSize',
      
      addOptions() {
        return {
          types: ['textStyle'],
          sizes: FONT_SIZES,
        }
      },
      
      addGlobalAttributes() {
        return [
          {
            types: this.options.types,
            attributes: {
              fontSize: {
                default: null,
                parseHTML: element => element.style.fontSize || null,
                renderHTML: attributes => {
                  if (!attributes.fontSize) return {}
                  return { style: `font-size: ${attributes.fontSize}` }
                },
              },
            },
          },
        ]
      },
      
      addCommands() {
        return {
          setFontSize: fontSize => ({ chain }) => {
            return chain().setMark('textStyle', { fontSize }).run()
          },
          unsetFontSize: () => ({ chain }) => {
            return chain().setMark('textStyle', { fontSize: null }).removeEmptyTextStyle().run()
          },
        }
      },
    })

    // Initialize Tiptap editor with Y.js
    const initEditor = () => {
      editor.value = new Editor({
        extensions: [
          StarterKit.configure({
            // Disable history as Y.js provides its own undo/redo
            history: false,
          }),
          Underline,
          TextAlign.configure({
            types: ['heading', 'paragraph'],
          }),
          TextStyle,
          FontFamily,
          FontSize,
          Color,
          AIAutocomplete,
          // Y.js Collaboration
          Collaboration.configure({
            document: props.ydoc,
          }),
          // Collaborative cursors
          CollaborationCursor.configure({
            provider: props.provider,
            user: props.user,
          }),
        ],
        editorProps: {
          attributes: {
            class: 'prose prose-lg max-w-none focus:outline-none min-h-full',
            spellcheck: 'false',
          },
        },
        onCreate: ({ editor }) => {
          // Wait for provider to sync before checking for initial content
          if (props.provider) {
            const syncHandler = (synced) => {
              if (synced) {
                setTimeout(() => {
                  const fragment = props.ydoc.getXmlFragment('default')
                  const hasYjsContent = fragment.toString().length > 0
                  const hasInitialHtml = props.initialContent && props.initialContent.length > 0
                  
                  if (!hasYjsContent && hasInitialHtml) {
                    editor.commands.setContent(props.initialContent)
                  }
                }, 500)
                
                props.provider.off('sync', syncHandler)
              }
            }
            
            props.provider.on('sync', syncHandler)
          }
          
          emit('ready', editor)
        },
        onUpdate: ({ editor, transaction }) => {
          // Only trigger AI for local changes, not remote Y.js updates
          const isRemoteUpdate = transaction.getMeta('y-sync$')
          if (isRemoteUpdate) {
            return
          }

          // Clear any existing timer
          if (aiTimer) clearTimeout(aiTimer)
          
          // Clear existing suggestion
          if (aiSuggestion.value) {
            const { state, view } = editor
            const tr = state.tr.setMeta('clearAISuggestion', true)
            view.dispatch(tr)
            aiSuggestion.value = null
          }
          
          const text = editor.getText()
          lastLocalText.value = text
          
          // Trigger AI after 3 seconds of inactivity (only if assisted writing is enabled)
          if (text.trim().length > 0 && isAssistedWritingEnabled.value) {
            aiTimer = setTimeout(async () => {
              isLoadingAI.value = true
              
              try {
                // Get cursor position and extract 100 words before it
                const cursorPos = editor.state.selection.from
                const textBeforeCursor = editor.state.doc.textBetween(0, cursorPos, ' ')
                const words = textBeforeCursor.trim().split(/\s+/)
                const last100Words = words.slice(-100).join(' ')
                
                const selectedFilesForAI = props.selectedFiles.map(f => ({
                  fileId: f.fileId,
                  filename: f.filename,
                  mimeType: f.mimeType || 'application/octet-stream'
                }))
                
                console.log('AI Autocomplete Request:')
                console.log('  - Context (last 100 words):', last100Words)
                console.log('  - Selected files:', selectedFilesForAI.length, selectedFilesForAI)
                
                const response = await fetch(`${API_BASE_URL}/api/ai/autocomplete`, {
                  method: 'POST',
                  headers: { 
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${auth.getToken()}`
                  },
                  body: JSON.stringify({ 
                    userInput: last100Words,
                    mode: 'fast',
                    selectedFiles: selectedFilesForAI
                  })
                })
                
                if (response.ok) {
                  const data = await response.json()
                  const suggestion = data.suggestion || ''
                  
                  if (suggestion) {
                    aiSuggestion.value = suggestion
                    
                    // Add decoration with suggestion
                    const { state, view } = editor
                    const tr = state.tr.setMeta('addAISuggestion', {
                      pos: state.selection.from,
                      suggestion: suggestion
                    })
                    view.dispatch(tr)
                  }
                }
              } catch (error) {
                console.error('AI autocomplete error:', error)
              } finally {
                isLoadingAI.value = false
              }
            }, 3000)
          }
        },
      })
    }

    // Generic command executor
    const executeEditorCommand = (commandFn) => {
      if (!editor.value) return false
      commandFn(editor.value.chain().focus())
      return true
    }

    // Handle heading selection
    const setHeading = (event) => {
      const level = event.target.value
      executeEditorCommand((chain) => {
        if (level === '' || level === 'paragraph') {
          chain.setParagraph().run()
        } else {
          chain.toggleHeading({ level: parseInt(level) }).run()
        }
      })
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

    // Set font family
    const setFontFamily = (event) => {
      const fontFamily = event.target.value
      executeEditorCommand((chain) => chain.setFontFamily(fontFamily).run())
    }

    // Set font size
    const setFontSize = (event) => {
      const fontSize = event.target.value
      currentFontSize.value = fontSize
      executeEditorCommand((chain) => chain.setFontSize(fontSize).run())
    }

    // Font size utilities
    const changeFontSizeByStep = (step) => {
      const currentIndex = FONT_SIZES.indexOf(currentFontSize.value)
      const newIndex = Math.max(0, Math.min(FONT_SIZES.length - 1, currentIndex + step))
      const newSize = FONT_SIZES[newIndex]
      currentFontSize.value = newSize
      executeEditorCommand((chain) => chain.setFontSize(newSize).run())
    }

    const increaseFontSize = () => changeFontSizeByStep(1)
    const decreaseFontSize = () => changeFontSizeByStep(-1)

    // Set text color
    const setTextColor = (event) => {
      const color = event.target.value
      currentTextColor.value = color
      executeEditorCommand((chain) => chain.setColor(color).run())
    }

    // Initialize editor on mount
    initEditor()

    onBeforeUnmount(() => {
      editor.value?.destroy()
    })

    // Computed properties for toolbar
    const activeHeading = computed(() => getActiveHeading())
    const isActive = computed(() => ({
      bold: editor.value?.isActive('bold'),
      italic: editor.value?.isActive('italic'),
      underline: editor.value?.isActive('underline'),
      strike: editor.value?.isActive('strike'),
      code: editor.value?.isActive('code'),
      codeBlock: editor.value?.isActive('codeBlock'),
      bulletList: editor.value?.isActive('bulletList'),
      orderedList: editor.value?.isActive('orderedList'),
      textAlignLeft: editor.value?.isActive({ textAlign: 'left' }),
      textAlignCenter: editor.value?.isActive({ textAlign: 'center' }),
      textAlignRight: editor.value?.isActive({ textAlign: 'right' }),
      textAlignJustify: editor.value?.isActive({ textAlign: 'justify' }),
    }))
    const canUndo = computed(() => editor.value?.can().undo())
    const canRedo = computed(() => editor.value?.can().redo())
    const editorContentArea = ref(null)

    // Create toggle command
    const createToggleCommand = (command) => () => {
      executeEditorCommand((chain) => chain[command]().run())
    }

    // Toolbar action methods
    const toggleBold = createToggleCommand('toggleBold')
    const toggleItalic = createToggleCommand('toggleItalic')
    const toggleUnderline = createToggleCommand('toggleUnderline')
    const toggleStrike = createToggleCommand('toggleStrike')
    const toggleCode = createToggleCommand('toggleCode')
    const toggleCodeBlock = createToggleCommand('toggleCodeBlock')
    const toggleBulletList = createToggleCommand('toggleBulletList')
    const toggleOrderedList = createToggleCommand('toggleOrderedList')
    const undo = createToggleCommand('undo')
    const redo = createToggleCommand('redo')
    
    const setTextAlign = (alignment) => {
      executeEditorCommand((chain) => chain.setTextAlign(alignment).run())
    }
    
    const openTextColorPicker = () => {
      document.getElementById('text-color-input').click()
    }

    const handleTextColorChange = (event) => {
      setTextColor(event)
    }

    return {
      editor,
      activeHeading,
      isActive,
      canUndo,
      canRedo,
      editorContentArea,
      currentTextColor,
      currentFontSize,
      setHeading,
      toggleBold,
      toggleItalic,
      toggleUnderline,
      toggleStrike,
      toggleCode,
      toggleCodeBlock,
      toggleBulletList,
      toggleOrderedList,
      setTextAlign,
      undo,
      redo,
      setFontFamily,
      setFontSize,
      increaseFontSize,
      decreaseFontSize,
      setTextColor,
      openTextColorPicker,
      handleTextColorChange,
    }
  },
}
</script>
