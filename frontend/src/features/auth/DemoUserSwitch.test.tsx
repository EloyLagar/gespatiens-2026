import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import { DemoUserSwitch } from './DemoUserSwitch'

const auth = {
  user: { id: 'u-1', name: 'Marta Serra', email: 'marta.serra@vientos.es', speciality: 'ADMINISTRATOR' },
  login: vi.fn(() => Promise.resolve()),
}
vi.mock('../../auth/AuthContext', () => ({ useAuth: () => auth }))
const demoAccounts = vi.fn()
vi.mock('../../api/demo', () => ({ demoAccounts: () => demoAccounts() }))
const navigate = vi.fn()
vi.mock('react-router-dom', async (original) => ({
  ...(await original<typeof import('react-router-dom')>()),
  useNavigate: () => navigate,
}))

const ACCOUNTS = [
  { name: 'Marta Serra', email: 'marta.serra@vientos.es', speciality: 'ADMINISTRATOR', password: 'demo1234' },
  { name: 'Jorge Vidal', email: 'jorge.vidal@vientos.es', speciality: 'PSYCHOLOGIST', password: 'demo1234' },
]

function show() {
  return render(
    <MemoryRouter>
      <DemoUserSwitch />
    </MemoryRouter>,
  )
}

describe('Cambiar de cuenta en la demo', () => {
  afterEach(() => vi.clearAllMocks())

  it('en un centro de verdad no sale', async () => {
    demoAccounts.mockRejectedValue(new Error('404'))
    const { container } = show()
    await vi.waitFor(() => expect(demoAccounts).toHaveBeenCalled())
    expect(container).toBeEmptyDOMElement()
  })

  it('entra con la otra cuenta y vuelve a los avisos', async () => {
    demoAccounts.mockResolvedValue(ACCOUNTS)
    show()
    const select = await screen.findByLabelText('Probar como')
    expect(select).toHaveValue('marta.serra@vientos.es')
    await userEvent.selectOptions(select, 'jorge.vidal@vientos.es')
    expect(auth.login).toHaveBeenCalledWith('jorge.vidal@vientos.es', 'demo1234')
    expect(navigate).toHaveBeenCalledWith('/avisos')
  })

  it('si no puede entrar, lo dice', async () => {
    demoAccounts.mockResolvedValue(ACCOUNTS)
    auth.login.mockRejectedValueOnce(new Error('401'))
    show()
    await userEvent.selectOptions(await screen.findByLabelText('Probar como'), 'jorge.vidal@vientos.es')
    expect(await screen.findByRole('alert')).toHaveTextContent('No se ha podido cambiar de cuenta')
    expect(navigate).not.toHaveBeenCalled()
  })
})
