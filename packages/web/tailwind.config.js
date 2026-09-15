/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        background: "#fff8f5",
        foreground: "#171329",
        primary: {
          DEFAULT: "#7c3aed",
          container: "#9b4dff",
          fixed: "#f0e7ff",
          dim: "#d6b8ff"
        },
        secondary: {
          DEFAULT: "#5f5e5e",
          container: "#e4e2e1",
          fixed: "#e4e2e1"
        },
        surface: {
          DEFAULT: "#f7f9f5",
          container: "#f0eded",
          "container-low": "#f0f4f0",
          "container-high": "#eae8e7",
          "container-highest": "#e4e2e1",
          "container-lowest": "#ffffff"
        },
        outline: {
          DEFAULT: "#738078",
          variant: "#c9d2cc"
        },
        error: "#d14343"
      },
      fontFamily: {
        headline: ["Manrope", "sans-serif"],
        body: ["DM Sans", "sans-serif"],
        mono: ["JetBrains Mono", "monospace"],
        display: ["Manrope", "sans-serif"]
      }
    }
  },
  plugins: []
};
