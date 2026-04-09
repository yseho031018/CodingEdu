/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/main/resources/templates/**/*.html',
    './src/main/resources/static/js/**/*.js',
  ],
  darkMode: ['selector', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        accent: {
          DEFAULT: '#6B5CE7',
          hover:   '#5A4BD6',
          soft:    '#EDE9FE',
        },
        amber: {
          brand:   '#F59E0B',
          soft:    '#FEF3C7',
        },
        ink: {
          1: '#1C1917',
          2: '#5C5750',
          3: '#9B9490',
        },
        surface: {
          DEFAULT: '#FFFFFF',
          section: '#F4EFE8',
          card:    '#FFFFFF',
          dark:    '#1C1B1A',
        },
        success: { DEFAULT: '#059669', soft: '#ECFDF5' },
        danger:  { DEFAULT: '#DC2626', soft: '#FEF2F2' },
        warning: { DEFAULT: '#D97706', soft: '#FFFBEB' },
      },
      fontFamily: {
        sans: ['Inter', 'Apple SD Gothic Neo', 'Malgun Gothic', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'Consolas', 'monospace'],
      },
      borderRadius: {
        '4xl': '2rem',
      },
      boxShadow: {
        card:        '0 1px 4px rgba(0,0,0,0.06)',
        'card-md':   '0 4px 24px rgba(0,0,0,0.09)',
        'card-lg':   '0 20px 64px rgba(0,0,0,0.11)',
        'accent-sm': '0 2px 12px rgba(107,92,231,0.25)',
        'accent-md': '0 4px 24px rgba(107,92,231,0.32)',
      },
      transitionTimingFunction: {
        spring: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
      },
      animation: {
        'fade-up':  'fadeUp 0.55s cubic-bezier(0.22,1,0.36,1) forwards',
        'fade-in':  'fadeIn 0.4s ease forwards',
      },
      keyframes: {
        fadeUp: {
          '0%':   { opacity: '0', transform: 'translateY(20px) scale(0.98)' },
          '100%': { opacity: '1', transform: 'translateY(0) scale(1)' },
        },
        fadeIn: {
          '0%':   { opacity: '0' },
          '100%': { opacity: '1' },
        },
      },
    },
  },
  plugins: [],
};
