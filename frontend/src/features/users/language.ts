import { changeMyLanguage } from '../../api/users'
import { useAuth } from '../../auth/AuthContext'
import type { Language } from '../../i18n'

// RNF9: guarda en el perfil el idioma que el empleado acaba de elegir. Si no se puede guardar (sin red), la
// pantalla ya ha cambiado de idioma y solo se pierde que lo recuerde en otro equipo
export function useSaveLanguage() {
  const { replaceUser } = useAuth()
  return (language: Language) => {
    changeMyLanguage(language === 'en' ? 'EN' : 'ES')
      .then(replaceUser)
      .catch(() => {})
  }
}
