import type { Patient } from '../../api/types'

// Sin tildes ni mayúsculas: "garcia" encuentra a "García"
function normalize(text: string): string {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim()
}

// Busca por nombre, apellidos, DNI o número de plaza. Vale para la ficha y para el residente de un informe
export function matches(patient: Pick<Patient, 'name' | 'surname' | 'dni' | 'number'>, query: string): boolean {
  const q = normalize(query)
  if (!q) return true
  // Uno o dos dígitos son un número de plaza (1-32), no un trozo de DNI: "5" no debe encontrar al 15
  if (/^\d{1,2}$/.test(q)) return patient.number === Number(q)
  const haystack = normalize(`${patient.name} ${patient.surname} ${patient.dni}`)
  return q.split(/\s+/).every((word) => haystack.includes(word))
}
