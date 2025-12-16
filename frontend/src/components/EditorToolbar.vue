<template>
  <div class="toolbar flex items-center border-b border-border-opa justify-center gap-1 p-2 bg-surface-light flex-shrink-0 sticky top-0 z-10 flex-wrap px-12 w-[90%] self-center">
    <!-- Headings & Normal Text -->
    <select 
      @change="$emit('set-heading', $event)"
      class="toolbar-select"
      :value="activeHeading"
    >
      <option value="">Normal text</option>
      <option value="1">Heading 1</option>
      <option value="2">Heading 2</option>
      <option value="3">Heading 3</option>
      <option value="4">Heading 4</option>
      <option value="5">Heading 5</option>
      <option value="6">Heading 6</option>
    </select>

    <!-- Separator -->
    <div class="w-px h-6 bg-border-opa mx-2"></div>

    <!-- Font Family -->
    <select 
      @change="$emit('set-font-family', $event)"
      class="toolbar-select"
      title="Font Family"
    >
      <option value="">Default</option>
      <option value="Arial, sans-serif">Arial</option>
      <option value="'Times New Roman', serif">Times New Roman</option>
      <option value="'Courier New', monospace">Courier New</option>
      <option value="Georgia, serif">Georgia</option>
      <option value="Verdana, sans-serif">Verdana</option>
      <option value="'Comic Sans MS', cursive">Comic Sans</option>
    </select>

    <!-- Separator -->
    <div class="w-px h-6 bg-border-opa mx-2"></div>

    <!-- Font Size -->
    <div class="flex items-center gap-1">
      <button
        @click="$emit('decrease-font-size')"
        class="toolbar-button"
        title="Decrease font size"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19 13H5v-2h14v2z"/>
        </svg>
      </button>
      
      <select 
        @change="$emit('set-font-size', $event)"
        :value="currentFontSize"
        class="toolbar-select w-16 text-center"
        title="Font Size"
      >
        <option value="8pt">8</option>
        <option value="9pt">9</option>
        <option value="10pt">10</option>
        <option value="11pt">11</option>
        <option value="12pt">12</option>
        <option value="14pt">14</option>
        <option value="18pt">18</option>
        <option value="24pt">24</option>
        <option value="30pt">30</option>
        <option value="36pt">36</option>
        <option value="48pt">48</option>
        <option value="60pt">60</option>
        <option value="72pt">72</option>
        <option value="96pt">96</option>
      </select>

      <button
        @click="$emit('increase-font-size')"
        class="toolbar-button"
        title="Increase font size"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
        </svg>
      </button>
    </div>

    <!-- Separator -->
    <div class="w-px h-6 bg-border-opa mx-2"></div>

    <!-- Text Formatting -->
    <button
      @click="$emit('toggle-bold')"
      :class="{ 'is-active': isActive?.bold }"
      class="toolbar-button"
      title="Bold"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M15.6 10.79c.97-.67 1.65-1.77 1.65-2.79 0-2.26-1.75-4-4-4H7v14h7.04c2.09 0 3.71-1.7 3.71-3.79 0-1.52-.86-2.82-2.15-3.42zM10 6.5h3c.83 0 1.5.67 1.5 1.5s-.67 1.5-1.5 1.5h-3v-3zm3.5 9H10v-3h3.5c.83 0 1.5.67 1.5 1.5s-.67 1.5-1.5 1.5z"/>
      </svg>
    </button>

    <button
      @click="$emit('toggle-italic')"
      :class="{ 'is-active': isActive?.italic }"
      class="toolbar-button"
      title="Italic"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M10 4v3h2.21l-3.42 8H6v3h8v-3h-2.21l3.42-8H18V4z"/>
      </svg>
    </button>

    <button
      @click="$emit('toggle-underline')"
      :class="{ 'is-active': isActive?.underline }"
      class="toolbar-button"
      title="Underline"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 17c3.31 0 6-2.69 6-6V3h-2.5v8c0 1.93-1.57 3.5-3.5 3.5S8.5 12.93 8.5 11V3H6v8c0 3.31 2.69 6 6 6zm-7 2v2h14v-2H5z"/>
      </svg>
    </button>

    <button
      @click="$emit('toggle-strike')"
      :class="{ 'is-active': isActive?.strike }"
      class="toolbar-button"
      title="Strikethrough"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M10 19h4v-3h-4v3zM5 4v3h5v3h4V7h5V4H5zM3 14h18v-2H3v2z"/>
      </svg>
    </button>

    <!-- Separator -->
    <div class="w-px h-6 bg-border-opa mx-2"></div>

    <!-- Text Color -->
    <div class="flex items-center gap-1">
      <div class="relative">
        <label class="toolbar-button flex items-center gap-1 cursor-pointer" title="Text Color">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M11 3L5.5 17h2.25l1.12-3h6.25l1.12 3h2.25L13 3h-2zm-1.38 9L12 5.67 14.38 12H9.62z"/>
          </svg>
          <div class="w-4 h-1 rounded" :style="{ backgroundColor: currentTextColor }"></div>
          <input 
            type="color" 
            :value="currentTextColor"
            @input="$emit('set-text-color', $event)"
            class="absolute opacity-0 w-0 h-0"
          />
        </label>
      </div>
    </div>

    <!-- Separator -->
    <div class="w-px h-6 bg-border-opa mx-2"></div>

    <!-- Text Alignment -->
    <button
      @click="$emit('set-text-align', 'left')"
      :class="{ 'is-active': isActive?.textAlignLeft }"
      class="toolbar-button"
      title="Align Left"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M15 15H3v2h12v-2zm0-8H3v2h12V7zM3 13h18v-2H3v2zm0 8h18v-2H3v2zM3 3v2h18V3H3z"/>
      </svg>
    </button>

    <button
      @click="$emit('set-text-align', 'center')"
      :class="{ 'is-active': isActive?.textAlignCenter }"
      class="toolbar-button"
      title="Align Center"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M7 15v2h10v-2H7zm-4 6h18v-2H3v2zm0-8h18v-2H3v2zm4-6v2h10V7H7zM3 3v2h18V3H3z"/>
      </svg>
    </button>

    <button
      @click="$emit('set-text-align', 'right')"
      :class="{ 'is-active': isActive?.textAlignRight }"
      class="toolbar-button"
      title="Align Right"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M3 21h18v-2H3v2zm6-4h12v-2H9v2zm-6-4h18v-2H3v2zm6-4h12V7H9v2zM3 3v2h18V3H3z"/>
      </svg>
    </button>

    <button
      @click="$emit('set-text-align', 'justify')"
      :class="{ 'is-active': isActive?.textAlignJustify }"
      class="toolbar-button"
      title="Justify"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M3 21h18v-2H3v2zm0-4h18v-2H3v2zm0-4h18v-2H3v2zm0-4h18V7H3v2zm0-6v2h18V3H3z"/>
      </svg>
    </button>

    <!-- Separator -->
    <div class="w-px h-6 bg-border-opa mx-2"></div>

    <!-- Lists -->
    <button
      @click="$emit('toggle-bullet-list')"
      :class="{ 'is-active': isActive?.bulletList }"
      class="toolbar-button"
      title="Bullet List"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M4 10.5c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5zm0-6c-.83 0-1.5.67-1.5 1.5S3.17 7.5 4 7.5 5.5 6.83 5.5 6 4.83 4.5 4 4.5zm0 12c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5zM7 19h14v-2H7v2zm0-6h14v-2H7v2zm0-8v2h14V5H7z"/>
      </svg>
    </button>

    <button
      @click="$emit('toggle-ordered-list')"
      :class="{ 'is-active': isActive?.orderedList }"
      class="toolbar-button"
      title="Numbered List"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M2 17h2v.5H3v1h1v.5H2v1h3v-4H2v1zm1-9h1V4H2v1h1v3zm-1 3h1.8L2 13.1v.9h3v-1H3.2L5 10.9V10H2v1zm5-6v2h14V5H7zm0 14h14v-2H7v2zm0-6h14v-2H7v2z"/>
      </svg>
    </button>

    <!-- Separator -->
    <div class="w-px h-6 bg-border-opa mx-2"></div>

    <!-- Code & Code Block -->
    <button
      @click="$emit('toggle-code')"
      :class="{ 'is-active': isActive?.code }"
      class="toolbar-button"
      title="Inline Code"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"/>
      </svg>
    </button>

    <button
      @click="$emit('toggle-code-block')"
      :class="{ 'is-active': isActive?.codeBlock }"
      class="toolbar-button"
      title="Code Block"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z"/>
        <path d="M9 8.5L5.5 12 9 15.5l1-1L7.5 12 10 9.5zm6 0l3.5 3.5-3.5 3.5-1-1L16.5 12 14 9.5z" opacity="0.7"/>
      </svg>
    </button>

    <!-- Separator -->
    <div class="w-px h-6 bg-border-opa mx-2"></div>

    <!-- Undo/Redo -->
    <button
      @click="$emit('undo')"
      :disabled="!canUndo"
      class="toolbar-button"
      title="Undo"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z"/>
      </svg>
    </button>

    <button
      @click="$emit('redo')"
      :disabled="!canRedo"
      class="toolbar-button"
      title="Redo"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M18.4 10.6C16.55 8.99 14.15 8 11.5 8c-4.65 0-8.58 3.03-9.96 7.22L3.9 16c1.05-3.19 4.05-5.5 7.6-5.5 1.95 0 3.73.72 5.12 1.88L13 16h9V7l-3.6 3.6z"/>
      </svg>
    </button>
  </div>
</template>

<script>
export default {
  name: 'EditorToolbar',
  props: {
    activeHeading: String,
    currentFontSize: String,
    currentTextColor: String,
    isActive: Object,
    canUndo: Boolean,
    canRedo: Boolean,
  },
  emits: [
    'set-heading',
    'set-font-family',
    'set-font-size',
    'increase-font-size',
    'decrease-font-size',
    'toggle-bold',
    'toggle-italic',
    'toggle-underline',
    'toggle-strike',
    'set-text-color',
    'set-text-align',
    'toggle-bullet-list',
    'toggle-ordered-list',
    'toggle-code',
    'toggle-code-block',
    'undo',
    'redo',
  ],
}
</script>
