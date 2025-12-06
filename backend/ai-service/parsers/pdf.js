/**
 * PDF file parser
 * Extracts text from PDF files (first 10 pages for speed)
 */

const pdfjsLib = require('pdfjs-dist/legacy/build/pdf.js');

function canParse(fileName, mimeType) {
  return fileName.toLowerCase().endsWith('.pdf') || 
         mimeType === 'application/pdf';
}

async function parse(buffer) {
  try {
    const uint8Array = new Uint8Array(buffer);
    const loadingTask = pdfjsLib.getDocument({ data: uint8Array });
    const pdf = await loadingTask.promise;
    let fullText = '';
    
    // Parse all pages
    for (let i = 1; i <= pdf.numPages; i++) {
      const page = await pdf.getPage(i);
      const content = await page.getTextContent();
      const pageText = content.items.map(item => item.str).join(' ');
      fullText += pageText + '\n';
    }
    
    return fullText;
  } catch (error) {
    console.error('PDF parsing failed:', error.message);
    return '';
  }
}

module.exports = { canParse, parse };
