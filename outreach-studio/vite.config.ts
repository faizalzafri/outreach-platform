import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import checker from 'vite-plugin-checker'
import { TanStackRouterVite } from '@tanstack/router-plugin/vite'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    TanStackRouterVite({
      routesDirectory: './src/routes',
      generatedRouteTree: './src/routeTree.gen.ts',
      quoteStyle: 'single',
    }),
    react(),
    checker({
      typescript: {
        tsconfigPath: './tsconfig.app.json',
      },
    }),
  ],
  resolve: {
    alias: {
      '@/components': path.resolve(__dirname, 'src/components'),
      '@/lib': path.resolve(__dirname, 'src/lib'),
      '@/hooks': path.resolve(__dirname, 'src/hooks'),
      '@/routes': path.resolve(__dirname, 'src/routes'),
      '@/types': path.resolve(__dirname, 'src/types'),
      '@/stores': path.resolve(__dirname, 'src/stores'),
      '@/test': path.resolve(__dirname, 'src/test'),
    },
  },
  server: {
    // HMR is enabled by default in Vite dev mode
    proxy: {
      '/api': {
        target: 'http://localhost:7093',
        changeOrigin: true,
      },
      '/oauth2': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        // Content-hashed filenames for cache busting
        entryFileNames: 'assets/[name]-[hash].js',
        chunkFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash].[ext]',
      },
    },
  },
})
