import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Sin los "globals" de Vitest, Testing Library no desmonta solo lo que renderiza cada test
afterEach(cleanup)
