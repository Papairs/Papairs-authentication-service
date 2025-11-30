const express = require('express');
const cors = require('cors');
const { OpenAI } = require('openai');
const fs = require('fs');
const path = require('path');
const fastMode = require('./services/fastMode');
const advancedMode = require('./services/advancedMode');

const app = express();
const PORT = process.env.PORT || 3001;
const DOCS_SERVICE_URL = 'http://docs-service:8082';
const TIMEOUT = 30000;

app.use(cors({
  origin: ['http://localhost:3000', 'http://localhost:8080'],
  credentials: true
}));
app.use(express.json());

const openai = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
});

function loadSystemPrompt() {
  const defaultPrompt = 'You are a helpful AI assistant providing code autocompletion suggestions. When files are provided, analyze their content to give context-aware suggestions.';
  try {
    const promptPath = path.join(__dirname, 'prompt.txt');
    if (fs.existsSync(promptPath)) {
      const prompt = fs.readFileSync(promptPath, 'utf-8');
      console.log('Loaded custom system prompt from prompt.txt');
      return prompt;
    }
  } catch (error) {
    console.error('Failed to load prompt.txt, using default:', error.message);
  }
  return defaultPrompt;
}

const systemPrompt = loadSystemPrompt();

app.get('/health', (req, res) => {
  res.json({ status: 'healthy', service: 'AI Service' });
});

app.post('/autocomplete', async (req, res) => {
  try {
    const { prompt, userInput, mode = 'fast' } = req.body;
    const inputText = prompt || userInput;

    if (!inputText || typeof inputText !== 'string' || inputText.trim() === '') {
      return res.status(400).json({ error: 'Prompt is required and must be a non-empty string' });
    }

    const startTime = Date.now();
    console.log(`[${mode.toUpperCase()}] Request: ${inputText.substring(0, 50)}...`);

    let suggestion;
    
    if (mode === 'advanced') {
      suggestion = await Promise.race([
        advancedMode.handleRequest(openai, systemPrompt, DOCS_SERVICE_URL, req),
        new Promise((_, reject) => 
          setTimeout(() => reject(new Error('Advanced mode timeout')), 120000)
        )
      ]);
    } else {
      suggestion = await Promise.race([
        fastMode.handleRequest(openai, systemPrompt, DOCS_SERVICE_URL, req),
        new Promise((_, reject) => 
          setTimeout(() => reject(new Error('Fast mode timeout')), TIMEOUT)
        )
      ]);
    }

    const elapsed = Date.now() - startTime;
    console.log(`[${mode.toUpperCase()}] Response in ${elapsed}ms`);

    res.json({ suggestion });
  } catch (error) {
    console.error('Error:', error.message);
    res.status(500).json({ error: error.message });
  }
});

app.listen(PORT, () => {
  console.log(`AI Service running on port ${PORT}`);
  console.log(`Docs Service URL: ${DOCS_SERVICE_URL}`);
  console.log(`Modes: fast (gpt-4o-mini), advanced (gpt-4o + file search)`);
});
