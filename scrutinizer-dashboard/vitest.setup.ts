import '@testing-library/jest-dom/vitest'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './src/__tests__/mocks/server'

// jsdom doesn't implement ResizeObserver, but Recharts (PostureTrendChart) and
// MUI DataGrid both rely on it. Provide a minimal no-op polyfill.
class ResizeObserverPolyfill {
  observe() {}
  unobserve() {}
  disconnect() {}
}
// @ts-expect-error attach to global for jsdom tests
globalThis.ResizeObserver = globalThis.ResizeObserver || ResizeObserverPolyfill

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
