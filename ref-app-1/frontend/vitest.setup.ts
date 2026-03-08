import '@testing-library/jest-dom/vitest'
import {afterAll, afterEach, beforeAll} from 'vitest'
import {server} from './src/__tests__/mocks/node'

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
