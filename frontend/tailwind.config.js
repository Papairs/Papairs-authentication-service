/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        accent: 'var(--accent)',
        red: 'var(--red)',
        
        surface: {
          light: 'var(--surface-light)',
          'light-secondary': 'var(--surface-light-secondary)',
          dark: 'var(--surface-dark)',
          'dark-secondary': 'var(--surface-dark-secondary)',
        },
        
        content: {
          primary: 'var(--content-primary)',
          secondary: 'var(--content-secondary)',
          inverse: 'var(--content-inverse)',
          black: 'var(--content-black)',
          white: 'var(--content-white)',
        },
        
        border: {
          light: 'var(--border-light)',
          dark: 'var(--border-dark)',
          'light-subtle': 'var(--border-light-subtle)',
          'dark-subtle': 'var(--border-dark-subtle)',
          'opa': 'var(--border-opa)',
        },
      },
    },
  },
  plugins: [],
}