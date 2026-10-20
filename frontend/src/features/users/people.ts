// Cada empleado tiene una inicial y un tinte pálido propio y estable (siempre el mismo para la misma
// persona). Es identidad, no dato: los tintes son claros y la inicial va en tinta oscura (contraste > 7:1)
const TINTS = ['#e5f6ff', '#defbe6', '#fff1f1', '#f2f4f8', '#fcf4d6', '#d9fbfb', '#ffe2ec']

export function tintFor(id: string): string {
  let hash = 0
  for (const char of id) hash = (hash * 31 + char.charCodeAt(0)) >>> 0
  return TINTS[hash % TINTS.length]
}

export function initialOf(name: string): string {
  return name.trim().charAt(0).toUpperCase() || '?'
}
