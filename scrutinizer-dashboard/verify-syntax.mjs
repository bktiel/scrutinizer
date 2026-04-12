import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

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

console.log('Verifying TypeScript files exist and are readable...\n');

let allGood = true;
files.forEach(file => {
  const filePath = path.join(__dirname, file);
  try {
    const content = fs.readFileSync(filePath, 'utf-8');
    const lines = content.split('\n').length;
    console.log(`✓ ${file} (${lines} lines)`);
  } catch (e) {
    console.error(`✗ ${file}: ${e.message}`);
    allGood = false;
  }
});

if (allGood) {
  console.log('\n✓ All files verified successfully!');
  process.exit(0);
} else {
  console.log('\n✗ Some files could not be read');
  process.exit(1);
}
