import { Button } from '@carbon/react'
import { useEffect, useState, type ChangeEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { getMySignature, removeMySignature, uploadMySignature } from '../../api/users'
import { ErrorNotice, LoadingRows } from '../../shared/ui'

// Debe coincidir con Signature.MAX_BYTES en el dominio
const MAX_BYTES = 1024 * 1024

// La firma escaneada que se imprime junto al nombre en los informes. La memoria final: "el empleado por cuenta
// propia suba una imagen de su firma la cual se empleará para los informes"
export function SignatureSection() {
  const { t } = useTranslation()
  const [image, setImage] = useState<Blob | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [invalid, setInvalid] = useState<string | null>(null)
  const [url, setUrl] = useState<string | null>(null)

  useEffect(() => {
    getMySignature()
      .then(setImage)
      .catch(setError)
      .finally(() => setLoading(false))
  }, [])

  // La imagen se enseña desde memoria: la firma no se deja en una URL que otro pueda abrir
  useEffect(() => {
    if (!image) {
      setUrl(null)
      return
    }
    const objectUrl = URL.createObjectURL(image)
    setUrl(objectUrl)
    return () => URL.revokeObjectURL(objectUrl)
  }, [image])

  async function upload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = '' // Para poder volver a elegir el mismo fichero
    if (!file) return
    if (file.type !== 'image/png') return setInvalid(t('profile.signature.onlyPng'))
    if (file.size > MAX_BYTES) return setInvalid(t('profile.signature.tooBig'))
    setInvalid(null)
    setBusy(true)
    setError(null)
    try {
      await uploadMySignature(file)
      setImage(file)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  async function remove() {
    setBusy(true)
    setError(null)
    try {
      await removeMySignature()
      setImage(null)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="settings-row" aria-labelledby="my-signature-title">
      <div className="settings-row__label">
        <h2 id="my-signature-title">{t('profile.signature.title')}</h2>
        <p>{t('profile.signature.help')}</p>
      </div>
      <div className="settings-row__panel">
        <ErrorNotice error={error} title={t('form.notSaved')} />
        {loading ? (
          <LoadingRows rows={1} label={t('common.loading')} />
        ) : (
          <>
            <div className="signature-preview">
              {url ? (
                <img src={url} alt={t('profile.signature.alt')} />
              ) : (
                <p className="muted">{t('profile.signature.none')}</p>
              )}
            </div>
            {invalid && <p className="form-error" role="alert">{invalid}</p>}
            <div className="form-actions">
              {/* Un input de fichero de verdad, con su etiqueta como botón: funciona con teclado y lector */}
              <label className={`cds--btn cds--btn--tertiary${busy ? ' cds--btn--disabled' : ''}`} htmlFor="signature-file">
                {image ? t('profile.signature.replace') : t('profile.signature.upload')}
              </label>
              <input id="signature-file" className="visually-hidden" type="file" accept="image/png" onChange={upload} disabled={busy} />
              {image && (
                <Button kind="danger--ghost" onClick={remove} disabled={busy}>
                  {t('profile.signature.remove')}
                </Button>
              )}
            </div>
          </>
        )}
      </div>
    </section>
  )
}
