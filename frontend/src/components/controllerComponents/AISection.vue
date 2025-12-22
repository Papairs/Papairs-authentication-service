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
        autocompletion: { userInput: 'The weather today is' },
        flashcards: { 
          content: 'The French Revolution was a period of radical political and societal change in France that began with the Estates General of 1789 and ended with the formation of the French Consulate in November 1799. Many of its ideas are considered fundamental principles of liberal democracy, while its values and institutions remain central to French political discourse.',
          numberOfCards: 5
        }
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
    },
    
    flashcardFormFields() {
      return [
        {
          key: 'content',
          label: 'Content (educational text to generate flashcards from)',
          type: 'textarea',
          value: this.testForms.flashcards.content,
          placeholder: 'Enter at least 50 characters of educational content...',
          rows: 6
        },
        {
          key: 'numberOfCards',
          label: 'Number of Cards',
          type: 'number',
          value: this.testForms.flashcards.numberOfCards,
          min: 1,
          max: 20
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
    },

    async testFlashcardGeneration() {
      const content = this.testForms.flashcards.content.trim();
      const numberOfCards = parseInt(this.testForms.flashcards.numberOfCards);
      
      if (!content || content.length < 50) {
        alert('Please enter at least 50 characters of content');
        return;
      }
      
      if (!numberOfCards || numberOfCards < 1 || numberOfCards > 20) {
        alert('Number of cards must be between 1 and 20');
        return;
      }
      
      await this.testManager.executeTest('ai-flashcards', async () => {
        const response = await aiController.generateFlashcards({
          content,
          numberOfCards
        });
        
        // Format the response to show flashcards nicely
        if (response.flashcards && Array.isArray(response.flashcards)) {
          return {
            success: true,
            count: response.flashcards.length,
            flashcards: response.flashcards,
            formatted: response.flashcards.map((card, index) => ({
              number: index + 1,
              question: card.question,
              answer: card.answer
            }))
          };
        }
        
        return response;
      });
    }
  }
}
</script>

<template>
  <div class="space-y-6">
    <h2 class="text-2xl font-bold text-content-primary mb-4">
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

      <!-- Flashcard Generation -->
      <TestCard
        title="Generate Flashcards"
        description="POST /generate-flashcards - Generate flashcards from content"
        button-text="Generate Flashcards"
        :form-fields="flashcardFormFields"
        :is-loading="testManager.isTestLoading('ai-flashcards')"
        :result="formatResult('ai-flashcards')"
        @test="testFlashcardGeneration"
        @clear-result="clearResult('ai-flashcards')"
        @field-update="updateFormField('flashcards', $event)"
        class="lg:col-span-2"
      />
    </div>

    <!-- AI Service Info -->
    <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
      <div class="flex">
        <div class="flex-shrink-0">
          <span class="text-lg">🤖</span>
        </div>
        <div class="ml-3">
          <h3 class="text-sm font-medium text-blue-800">
            AI Service Information
          </h3>
          <div class="mt-2 text-sm text-blue-700">
            <p>• The AI service provides autocompletion suggestions using OpenAI's GPT model</p>
            <p>• It processes user input and returns completion suggestions with a maximum of 30 tokens</p>
            <p>• The service runs on port 3001 and requires an OpenAI API key to function</p>
            <p>• Flashcard generation uses GPT-4o-mini to create educational question-answer pairs from content</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

