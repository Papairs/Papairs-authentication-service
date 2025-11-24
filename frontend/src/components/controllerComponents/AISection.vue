<template>
  <div class="space-y-6">
    <h2 class="text-2xl font-bold text-content-primary dark:text-content-inverse mb-4">
      AI Service Tests
    </h2>
    
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Health Check -->
      <TestCard
        title="Health Check"
        description="POST /autocomplete (with test input)"
        button-text="Test AI Health"
        :is-loading="testManager.isTestLoading('ai-health')"
        :result="formatResult('ai-health')"
        @test="testAIHealth"
        @clear-result="clearResult('ai-health')"
      />

      <!-- Autocompletion -->
      <TestCard
        title="Get Autocompletion"
        description="POST /autocomplete"
        button-text="Test Autocompletion"
        :form-fields="autocompletionFormFields"
        :is-loading="testManager.isTestLoading('ai-autocomplete')"
        :result="formatResult('ai-autocomplete')"
        @test="testAutocompletion"
        @clear-result="clearResult('ai-autocomplete')"
        @field-update="updateFormField('autocompletion', $event)"
      />
    </div>

    <!-- AI Service Info -->
    <div class="bg-blue-50 dark:bg-blue-900 border border-blue-200 dark:border-blue-700 rounded-lg p-4">
      <div class="flex">
        <div class="flex-shrink-0">
          <span class="text-lg">🤖</span>
        </div>
        <div class="ml-3">
          <h3 class="text-sm font-medium text-blue-800 dark:text-blue-200">
            AI Service Information
          </h3>
          <div class="mt-2 text-sm text-blue-700 dark:text-blue-300">
            <p>• The AI service provides autocompletion suggestions using OpenAI's GPT model</p>
            <p>• It processes user input and returns completion suggestions with a maximum of 30 tokens</p>
            <p>• The service runs on port 3001 and requires an OpenAI API key to function</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import TestCard from './TestCard.vue';
import { aiController } from '@/controllers/index.js';

export default {
  name: 'AISection',
  components: {
    TestCard
  },
  props: {
    testManager: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      testForms: {
        autocompletion: { userInput: 'The weather today is' }
      }
    }
  },
  computed: {
    autocompletionFormFields() {
      return [
        {
          key: 'userInput',
          label: 'User Input (text to complete)',
          type: 'textarea',
          value: this.testForms.autocompletion.userInput,
          placeholder: 'Enter some text for AI to complete...'
        }
      ];
    }
  },
  methods: {
    formatResult(testKey) {
      const result = this.testManager.getResult(testKey);
      return result ? this.testManager.formatResult(result) : null;
    },
    
    clearResult(testKey) {
      this.testManager.clearResult(testKey);
    },

    updateFormField(formName, { key, value }) {
      if (this.testForms[formName]) {
        this.testForms[formName][key] = value;
      }
    },

    async testAIHealth() {
      await this.testManager.executeTest('ai-health', () => aiController.checkHealth());
    },

    async testAutocompletion() {
      if (!this.testForms.autocompletion.userInput.trim()) {
        alert('Please enter some text for autocompletion');
        return;
      }
      
      await this.testManager.executeTest('ai-autocomplete', () => 
        aiController.getAutocompletion(this.testForms.autocompletion)
      );
    }
  }
}
</script>