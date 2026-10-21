import { Button, Tag } from '@carbon/react'
import { Add } from '@carbon/icons-react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { listUsers } from '../../api/users'
import { useAuth } from '../../auth/AuthContext'
import { ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'
import { initialOf, tintFor } from './people'

// Pregunta de la pantalla: ¿quién trabaja en el centro y cómo localizarle?
export function EmployeesPage() {
  const { t } = useTranslation()
  const { isAdmin, user: me } = useAuth()
  const navigate = useNavigate()
  const { data, error, loading, reload } = useApi(listUsers, [])
  const users = data ?? []
  const admins = users.filter((u) => u.speciality === 'ADMINISTRATOR').length

  return (
    <>
      <PageHeader
        title={t('employees.title')}
        subtitle={data ? t('employees.subtitle', { count: users.length, admins }) : null}
        actions={
          // RF7: dar de alta empleados es cosa de la administración
          isAdmin && (
            <Button renderIcon={Add} onClick={() => navigate('/empleados/nuevo')}>
              {t('employees.add')}
            </Button>
          )
        }
      />

      <section className="list-section" aria-label={t('employees.title')}>
        {error ? (
          <ErrorNotice error={error} onRetry={reload} />
        ) : loading && !data ? (
          <LoadingRows label={t('common.loading')} />
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <caption className="visually-hidden">{t('employees.tableCaption')}</caption>
              <thead>
                <tr>
                  <th scope="col">{t('user.name')}</th>
                  <th scope="col">{t('user.speciality')}</th>
                  <th scope="col">{t('user.email')}</th>
                  <th scope="col">{t('user.phoneNumber')}</th>
                  {isAdmin && (
                    <th scope="col" className="col-action">
                      <span className="visually-hidden">{t('common.actions')}</span>
                    </th>
                  )}
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td className="col-identity">
                      <span className="person">
                        <span className="avatar" style={{ background: tintFor(u.id) }} aria-hidden="true">
                          {initialOf(u.name)}
                        </span>
                        <span>
                          {u.name}
                          {u.id === me?.id && <span className="you"> ({t('employees.you')})</span>}
                          {/* Invitado que aún no ha creado su contraseña desde el correo */}
                          {u.passwordPending && (
                            <Tag type="warm-gray" size="sm" className="person__tag">
                              {t('employees.pending')}
                            </Tag>
                          )}
                          <span className="row-sub">
                            {t(`speciality.${u.speciality}`)} · {u.phoneNumber}
                          </span>
                        </span>
                      </span>
                    </td>
                    <td className="col-wide">{t(`speciality.${u.speciality}`)}</td>
                    <td className="col-wide">
                      <a href={`mailto:${u.email}`}>{u.email}</a>
                    </td>
                    <td className="col-wide num">
                      <a href={`tel:${u.phoneNumber}`}>{u.phoneNumber}</a>
                    </td>
                    {isAdmin && (
                      <td className="col-action">
                        {u.id === me?.id ? (
                          <Link to="/perfil" className="text-action">{t('employees.myProfile')}</Link>
                        ) : (
                          <Link to={`/empleados/${u.id}`} className="text-action" aria-label={t('employees.editWho', { name: u.name })}>
                            {t('common.edit')}
                          </Link>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  )
}
