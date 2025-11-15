<template>
  <div class="test-card">
    <h3 class="test-title">{{ title }}</h3>
    <p class="test-description">{{ description }}</p>
    
    <!-- Form Fields -->
    <div v-if="formFields && formFields.length > 0" class="form-section">
      <div
        v-for="field in formFields"
        :key="field.key"
        class="form-group"
      >
        <label>{{ field.label }}:</label>
        <input
          v-if="field.type !== 'textarea'"
          :type="field.type || 'text'"
          :value="field.value"
          @input="updateField(field.key, $event.target.value)"
          class="form-input"
          :placeholder="field.placeholder"
        />
        <textarea
          v-else
          :value="field.value"
          @input="updateField(field.key, $event.target.value)"
          class="form-input h-20"
          :placeholder="field.placeholder"
        ></textarea>
      </div>
    </div>

    <!-- Test Button -->
    <button
      @click="executeTest"
      :disabled="isLoading"
      :class="[
        'test-button',
        buttonColor === 'danger' ? 'bg-red-600 hover:bg-red-700' : ''
      ]"
    >
      {{ isLoading ? loadingText : buttonText }}
    </button>

    <!-- Warning Message -->
    <p v-if="warning" class="text-red-600 dark:text-red-400 text-sm mb-2">
      ⚠️ {{ warning }}
    </p>

    <!-- Test Result -->
    <div
      v-if="result"
      class="test-result"
      :class="result.status"
    >
      <div class="flex justify-between items-start mb-2">
        <span class="font-medium">Result:</span>
        <button
          @click="clearResult"
          class="text-xs bg-gray-500 hover:bg-gray-600 text-white px-2 py-1 rounded"
        >
          Clear
        </button>
      </div>
      <pre>{{ JSON.stringify(result, null, 2) }}</pre>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TestCard',
  props: {
    title: {
      type: String,
      required: true
    },
    description: {
      type: String,
      required: true
    },
    formFields: {
      type: Array,
      default: () => []
    },
    isLoading: {
      type: Boolean,
      default: false
    },
    result: {
      type: Object,
      default: null
    },
    buttonText: {
      type: String,
      default: 'Test'
    },
    loadingText: {
      type: String,
      default: 'Testing...'
    },
    buttonColor: {
      type: String,
      default: 'primary',
      validator: (value) => ['primary', 'danger'].includes(value)
    },
    warning: {
      type: String,
      default: ''
    }
  },
  emits: ['test', 'clear-result', 'field-update'],
  methods: {
    executeTest() {
      if (!this.isLoading) {
        this.$emit('test');
      }
    },
    clearResult() {
      this.$emit('clear-result');
    },
    updateField(key, value) {
      this.$emit('field-update', { key, value });
    }
  }
}
</script>

<style scoped>
/* Test card styles */
.test-card {
  background-color: var(--surface-light-secondary);
  padding: 1.5rem;
  border-radius: 0.5rem;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
  transition: all 0.3s ease;
}

:global(.dark) .test-card {
  background-color: var(--surface-dark-secondary);
}

.test-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--content-primary);
  margin-bottom: 0.5rem;
}

:global(.dark) .test-title {
  color: var(--content-inverse);
}

.test-description {
  font-size: 0.875rem;
  color: var(--content-secondary);
  margin-bottom: 1rem;
  font-family: ui-monospace, SFMono-Regular, "SF Mono", Monaco, Inconsolata, "Liberation Mono", "Consolas", monospace;
  background-color: #f3f4f6;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
}

:global(.dark) .test-description {
  background-color: #374151;
}

.form-section {
  margin-bottom: 1rem;
}

.test-button {
  background-color: var(--accent);
  color: white;
  font-weight: 500;
  padding: 0.5rem 1rem;
  border-radius: 0.375rem;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  width: 100%;
  margin-bottom: 1rem;
}

.test-button:hover:not(:disabled) {
  background-color: #E66900;
}

.test-button:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  color: var(--content-primary);
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 0.25rem;
}

:global(.dark) .form-group label {
  color: var(--content-inverse);
}

.form-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border-light);
  border-radius: 0.375rem;
  background-color: var(--surface-light);
  color: var(--content-primary);
  outline: none;
  transition: border-color 0.3s ease;
}

.form-input:focus {
  border-color: var(--accent);
}

:global(.dark) .form-input {
  border-color: var(--border-dark);
  background-color: var(--surface-dark);
  color: var(--content-inverse);
}

.test-result {
  padding: 0.75rem;
  border-radius: 0.375rem;
  font-size: 0.75rem;
  overflow-x: auto;
  border: 1px solid;
}

.test-result.success {
  background-color: #dcfce7;
  border-color: #bbf7d0;
  color: #166534;
}

:global(.dark) .test-result.success {
  background-color: #14532d;
  border-color: #15803d;
  color: #bbf7d0;
}

.test-result.error {
  background-color: #fef2f2;
  border-color: #fecaca;
  color: #dc2626;
}

:global(.dark) .test-result.error {
  background-color: #7f1d1d;
  border-color: #dc2626;
  color: #fecaca;
}

.test-result pre {
  white-space: pre-wrap;
  word-break: break-words;
  margin: 0;
}
</style>