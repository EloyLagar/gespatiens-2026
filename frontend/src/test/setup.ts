import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Sin los "globals" de Vitest, Testing Library no desmonta solo lo que renderiza cada test
afterEach(cleanup)

// jsdom no trae ResizeObserver y algunos componentes de Carbon (Modal) lo usan al montarse
if (!('ResizeObserver' in globalThis)) {
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
}
