import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { useSesion } from '../lib/sesion'
import { Marca } from '../carta/Marca'

const DEMO = [
  { email: 'cocina@donameche.demo', rol: 'Cocina', que: 'pantalla de pedidos por estación' },
  { email: 'mozo@donameche.demo', rol: 'Mozo', que: 'mesas, confirmar y servir' },
  { email: 'caja@donameche.demo', rol: 'Caja', que: 'cobros y cierre del día' },
  { email: 'admin@donameche.demo', rol: 'Administración', que: 'todo, incluida la carta' },
]

export function Login() {
  const { entrar } = useSesion()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function ingresar(correo: string, clave: string) {
    setEnviando(true)
    setError(null)
    try {
      await entrar(correo, clave)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo iniciar sesión.')
    } finally {
      setEnviando(false)
    }
  }

  function enviar(e: FormEvent) {
    e.preventDefault()
    ingresar(email, password)
  }

  return (
    <div className="login">
      <div className="login-caja">
        <div className="login-marca">
          <Marca tam={44} />
          <div>
            <h1>Personal de La Sazón de Doña Meche</h1>
            <p>Cocina, mozos y caja.</p>
          </div>
        </div>
        <form className="login-form" onSubmit={enviar}>
          <label className="campo">
            <span>Correo</span>
            <input className="entrada" type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>
          <label className="campo">
            <span>Contraseña</span>
            <input className="entrada" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
          </label>
          {error && (
            <p className="error-bloque" role="alert">
              {error}
            </p>
          )}
          <button className="btn btn-primario btn-grande" disabled={enviando}>
            {enviando ? 'Entrando…' : 'Entrar'}
          </button>
        </form>
        <section className="login-demo" aria-labelledby="t-demo">
          <h2 id="t-demo">Probar la demo</h2>
          <p>
            Datos ficticios que se reinician cada día. Contraseña de todas las cuentas: <strong className="num">demo1234</strong>
          </p>
          <ul>
            {DEMO.map((d) => (
              <li key={d.email}>
                <button disabled={enviando} onClick={() => ingresar(d.email, 'demo1234')}>
                  <strong>{d.rol}</strong>
                  <span>{d.que}</span>
                </button>
              </li>
            ))}
          </ul>
        </section>
        <Link to="/" className="btn btn-texto btn-chico">
          <ArrowLeft size={16} /> Volver a la carta
        </Link>
      </div>
    </div>
  )
}
