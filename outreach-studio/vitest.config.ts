import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
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
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov', 'json-summary'],
      thresholds: {
        lines: 70,
        branches: 60,
      },
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/test/**', 'src/routeTree.gen.ts'],
    },
    reporters: ['default', 'junit'],
    outputFile: { junit: './test-results/junit.xml' },
  },
})
