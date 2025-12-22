/**
 * Fast Mode - Chat Completions API
 * Fast, lightweight autocomplete using GPT-4o-mini
 * Supports: text, PDF (local parsing)
 */

const axios = require('axios');
const { parseFile } = require('../parsers');

async function downloadFile(fileId, userId, docsServiceUrl) {
  const downloadUrl = `${docsServiceUrl}/api/docs/files/download/${fileId}`;
  const response = await axios.get(downloadUrl, {
    responseType: 'arraybuffer',
    headers: { 'X-User-Id': userId }
  });
  return Buffer.from(response.data);
}

async function processFile(file, userId, docsServiceUrl) {
  const fileName = file.filename || file.name || file.fileName;
  const fileId = file.fileId;
  const mimeType = file.mimeType || file.type || 'application/octet-stream';
  
  try {
    const buffer = await downloadFile(fileId, userId, docsServiceUrl);
    const content = await parseFile(buffer, fileName, mimeType);
    
    return content;
  } catch (error) {
    console.error(`Failed to process ${fileName}:`, error.message);
    return null;
  }
}

async function handleRequest(openai, systemPrompt, docsServiceUrl, req, userId) {
  const { prompt, userInput, selectedFiles = [] } = req.body;
  const inputText = prompt || userInput;

  // Process files in parallel
  let fileContext = '';
  if (selectedFiles.length > 0 && userId) {
    const filePromises = selectedFiles.map(file => processFile(file, userId, docsServiceUrl));
    const fileContents = await Promise.all(filePromises);
    const validContents = fileContents.filter(c => c !== null);
    
    if (validContents.length > 0) {
      fileContext = validContents.join('\n\n---\n\n');
    }
  }

  // Build messages
  const messages = [{ role: 'system', content: systemPrompt }];
  
  if (fileContext) {
    messages.push({
      role: 'user',
      content: `Context from uploaded files:\n\n${fileContext}\n\n---\n\nUser question: ${inputText}`
    });
  } else {
    messages.push({ role: 'user', content: inputText });
  }

  // Fast response with GPT-4o-mini
  const completion = await openai.chat.completions.create({
    model: 'gpt-4o-mini',
    messages,
    max_tokens: 300,
    temperature: 0.1, // Lower temperature for more consistent completions
  });

  return completion.choices[0].message.content;
}

module.exports = { handleRequest };
