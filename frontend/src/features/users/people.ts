// Cada empleado tiene una inicial y un tinte propio y estable (siempre el mismo para la misma persona). Es
// identidad, no dato. Los tintes son variables de app.scss: pálidos con tinta oscura en el tema claro y
// profundos con tinta clara en el oscuro (contraste > 7:1 en los dos)
const TINTS = [1, 2, 3, 4, 5, 6, 7].map((n) => `var(--person-${n})`)

export function tintFor(id: string): string {
  let hash = 0
  for (const char of id) hash = (hash * 31 + char.charCodeAt(0)) >>> 0
  return TINTS[hash % TINTS.length]
}

export function initialOf(name: string): string {
  return name.trim().charAt(0).toUpperCase() || '?'
}
