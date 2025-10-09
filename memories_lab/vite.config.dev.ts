import path from 'path';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    plugins: [react()],
    define: {
      'process.env.NODE_ENV': '"development"',
      'process.env': JSON.stringify(env),
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      proxy: {
        '/api': {
          // Local development: Run backend with `cd ../memorylab && ./gradlew bootRun`
          target: 'http://localhost:8080',
          changeOrigin: true,
          secure: false,
          // Production backend (currently down - 502 Bad Gateway)
          // target: 'https://mlab.snowytiger.me',
          // Alternative: Direct IP if domain is unavailable
          // target: 'http://54.180.3.34',
        },
      },
    },
  };
});
