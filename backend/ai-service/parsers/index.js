/**
 * File parser registry
 * Add new parsers here to extend supported file types
 */

const textParser = require('./text');
const pdfParser = require('./pdf');

// Register all parsers in order of priority
const PARSERS = [
  textParser,
  pdfParser,
  // Add new parsers here (e.g., docx, xlsx, images)
];

async function parseFile(buffer, fileName, mimeType) {
  for (const parser of PARSERS) {
    if (parser.canParse(fileName, mimeType)) {
      return await parser.parse(buffer);
    }
  }
  
  return null;
}

module.exports = { parseFile };

module.exports = { parseFile };
