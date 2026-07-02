const fs = require('fs');
const { baseParse } = require('@vue/compiler-core');
const content = fs.readFileSync('src/MainApp.vue', 'utf-8');
const { parse } = require('@vue/compiler-sfc');
const result = parse(content, { filename: 'MainApp.vue' });
const tmpl = result.descriptor.template;

// Try different small truncations
const sizes = [50, 100, 150, 200, 250, 300, 350, 400, 450, 500];
for (const len of sizes) {
  const snippet = tmpl.content.substring(0, len);
  const lastLt = snippet.lastIndexOf('<');
  const lastGt = snippet.lastIndexOf('>');
  const testContent = snippet + '</div></div></template>';
  try {
    baseParse(testContent);
    console.log('OK at length', len);
  } catch (e) {
    console.log('FAIL at length', len, '- last<:', lastLt, 'last>:', lastGt, '- error:', e.message);
    // Show the content around the issue
    if (lastLt > lastGt) {
      console.log('  Unclosed tag starting at:', JSON.stringify(snippet.substring(lastLt)));
    }
  }
}

// Show first 500 chars with line numbers
const lines = tmpl.content.substring(0, 500).split('\n');
for (let i = 0; i < lines.length; i++) {
  console.log('T' + (i+1) + ':', JSON.stringify(lines[i]));
}
