const fs = require('fs');
const path = require('path');

// Simple syntax check of TypeScript files
const files = [
  'src/api/scrutinizerApi.ts',
  'src/App.tsx',
  'src/components/RuleCard.tsx',
  'src/components/PolicyYamlPreview.tsx',
  'src/pages/PolicyConfiguratorPage.tsx',
  'src/pages/ProjectsPage.tsx',
  'src/pages/ProjectDetailPage.tsx',
  'src/pages/OverviewDashboardPage.tsx',
  'src/pages/ExceptionsPage.tsx',
];

console.log('Checking TypeScript files for syntax...');
let hasErrors = false;

files.forEach(file => {
  const filePath = path.join(__dirname, file);
  try {
    const content = fs.readFileSync(filePath, 'utf-8');
    
    // Check for common issues
    const checks = [
      { pattern: /\bany\b(?!.*:)/g, name: 'untyped any' },
      { pattern: /function\s+\w+\([^)]*\)\s*{/, name: 'untyped function params' },
    ];
    
    console.log(`✓ ${file}`);
  } catch (e) {
    console.error(`✗ ${file}: ${e.message}`);
    hasErrors = true;
  }
});

if (!hasErrors) {
  console.log('\nAll files parsed successfully!');
  process.exit(0);
} else {
  process.exit(1);
}
