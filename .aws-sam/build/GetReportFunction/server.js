import http from 'node:http';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateApplication } from './validator.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const webDir = path.join(__dirname, '..', 'web');

function send(res, status, contentType, body) {
  res.writeHead(status, { 'Content-Type': contentType, 'Cache-Control': 'no-store' });
  res.end(body);
}

const server = http.createServer(async (req, res) => {
  try {
    if (req.method === 'POST' && req.url === '/api/validate') {
      let raw = '';
      for await (const chunk of req) raw += chunk;
      const packet = JSON.parse(raw || '{}');
      return send(res, 200, 'application/json; charset=utf-8', JSON.stringify(validateApplication(packet)));
    }

    if (req.method === 'GET' && req.url === '/api/demo/broken') {
      const body = await fs.readFile(path.join(__dirname, '..', 'demo', 'application-broken.json'), 'utf8');
      return send(res, 200, 'application/json; charset=utf-8', body);
    }

    if (req.method === 'GET' && req.url === '/api/demo/fixed') {
      const body = await fs.readFile(path.join(__dirname, '..', 'demo', 'application-fixed.json'), 'utf8');
      return send(res, 200, 'application/json; charset=utf-8', body);
    }

    const requested = req.url === '/' ? '/index.html' : req.url;
    const filePath = path.normalize(path.join(webDir, requested));
    if (!filePath.startsWith(webDir)) return send(res, 400, 'text/plain', 'Bad request');
    const data = await fs.readFile(filePath);
    const ext = path.extname(filePath);
    const type = ext === '.html' ? 'text/html; charset=utf-8' : ext === '.js' ? 'text/javascript; charset=utf-8' : 'text/plain; charset=utf-8';
    return send(res, 200, type, data);
  } catch (error) {
    const status = error instanceof SyntaxError ? 400 : 500;
    return send(res, status, 'application/json; charset=utf-8', JSON.stringify({ error: error.message }));
  }
});

server.listen(process.env.PORT || 3000, () => {
  console.log(`SubmitReady running at http://localhost:${process.env.PORT || 3000}`);
});
