type GeneratedFile = { fileName: string; blob: Blob }

// Guarda en el equipo un fichero que ha generado la API (los informes en PDF), con su nombre
export function saveFile({ fileName, blob }: GeneratedFile) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  // Después del clic: si se revoca antes, algunos navegadores cancelan la descarga
  setTimeout(() => URL.revokeObjectURL(url), 0)
}

// Vista previa: abre el PDF en una pestaña nueva. La pestaña se abre antes de pedirlo, en el mismo clic,
// porque el navegador bloquea las ventanas que se abren después de esperar a la red. Si aun así la
// bloquea, se descarga
export async function previewFile(load: () => Promise<GeneratedFile>) {
  const tab = window.open('', '_blank')
  try {
    const file = await load()
    if (!tab) {
      saveFile(file)
      return
    }
    const url = URL.createObjectURL(file.blob)
    tab.location.href = url
    // La pestaña ya lo ha cargado; se libera al cabo de un rato
    setTimeout(() => URL.revokeObjectURL(url), 60_000)
  } catch (err) {
    tab?.close()
    throw err
  }
}
