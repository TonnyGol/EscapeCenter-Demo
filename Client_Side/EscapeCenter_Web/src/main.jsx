import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// Fonts are served from our own files, so visitors do not send their address to Google.
import '@fontsource/heebo/hebrew-400.css'
import '@fontsource/heebo/hebrew-500.css'
import '@fontsource/heebo/hebrew-700.css'
import '@fontsource/heebo/latin-400.css'
import '@fontsource/heebo/latin-500.css'
import '@fontsource/heebo/latin-700.css'
import '@fontsource/secular-one/hebrew-400.css'
import '@fontsource/secular-one/latin-400.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
