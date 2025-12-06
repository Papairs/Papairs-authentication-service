/**
 * Text file parser
 * Supports: .txt, .md, .json, .xml, .csv, .yaml, .log, code files
 */

const TEXT_EXTENSIONS = [
  '.txt', '.md', '.json', '.xml', '.csv', '.yaml', '.yml', '.log',
  '.js', '.ts', '.py', '.java', '.c', '.cpp', '.cs', '.rb', '.php',
  '.go', '.rs', '.sh', '.html', '.css', '.sql'
];

function canParse(fileName, mimeType) {
  const lowerFileName = fileName.toLowerCase();
  return mimeType.startsWith('text/') || 
         TEXT_EXTENSIONS.some(ext => lowerFileName.endsWith(ext));
}

async function parse(buffer) {
  return buffer.toString('utf-8');
}

module.exports = { canParse, parse };
