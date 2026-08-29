import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // Matches the backend's safe development CORS default.
  server: { port: 3000, strictPort: true },
});
