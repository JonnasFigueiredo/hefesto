import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        'bg-base': 'var(--bg-base)',
        'bg-elevated': 'var(--bg-elevated)',
        'bg-overlay': 'var(--bg-overlay)',
        'border-dim': 'var(--border-dim)',
        'border-default': 'var(--border)',
        'border-accent': 'var(--border-accent)',
        text: 'var(--text)',
        'text-dim': 'var(--text-dim)',
        'text-muted': 'var(--text-muted)',
        'accent-cyan': 'var(--accent-cyan)',
        'accent-amber': 'var(--accent-amber)',
        'accent-magenta': 'var(--accent-magenta)',
        'accent-green': 'var(--accent-green)',
      },
      fontFamily: {
        display: ['Rajdhani', 'system-ui', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'monospace'],
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      keyframes: {
        'pulse-dot': {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0.4' },
        },
        scanline: {
          '0%': { transform: 'translateY(-100%)' },
          '100%': { transform: 'translateY(100vh)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-1000px 0' },
          '100%': { backgroundPosition: '1000px 0' },
        },
        'spin-slow': {
          to: { transform: 'rotate(360deg)' },
        },
      },
      animation: {
        'pulse-dot': 'pulse-dot 1.5s ease-in-out infinite',
        scanline: 'scanline 8s linear infinite',
        shimmer: 'shimmer 2s linear infinite',
        'spin-slow': 'spin-slow 2s linear infinite',
      },
    },
  },
  plugins: [],
} satisfies Config
