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
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCursor from '@tiptap/extension-collaboration-cursor'
import { onBeforeUnmount, ref, computed } from 'vue'
import EditorToolbar from './EditorToolbar.vue'
import EditorContentArea from './EditorContent.vue'
import './TiptapEditor.css'

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
    }
  },
  emits: ['ready'],
  setup(props, { emit }) {
    // Constants
    const DEFAULT_TEXT_COLOR = '#000000'
    const DEFAULT_FONT_SIZE = '12pt'
    const FONT_SIZES = ['8pt', '9pt', '10pt', '11pt', '12pt', '14pt', '18pt', '24pt', '30pt', '36pt', '48pt', '60pt', '72pt', '96pt']
    
    // State
    const editor = ref(null)
    const currentTextColor = ref(DEFAULT_TEXT_COLOR)
    const currentFontSize = ref(DEFAULT_FONT_SIZE)

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
