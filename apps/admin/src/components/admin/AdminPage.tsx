import type { ReactNode } from 'react'
import './AdminPage.css'

type AdminPageProps = {
  children: ReactNode
  className?: string
}

type AdminPageHeaderProps = {
  title: ReactNode
  description?: ReactNode
  actions?: ReactNode
}

type AdminFilterBarProps = {
  children: ReactNode
  extra?: ReactNode
  className?: string
}

type AdminContentCardProps = {
  children: ReactNode
  title?: ReactNode
  description?: ReactNode
  extra?: ReactNode
  flush?: boolean
  className?: string
}

type AdminCountProps = {
  children: ReactNode
}

function joinClassNames(...names: Array<string | false | null | undefined>) {
  return names.filter(Boolean).join(' ')
}

export function AdminPage({ children, className }: AdminPageProps) {
  return <div className={joinClassNames('admin-page', className)}>{children}</div>
}

export function AdminPageHeader({ title, description, actions }: AdminPageHeaderProps) {
  return (
    <header className="admin-page-header">
      <div className="admin-page-header__copy">
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {actions && <div className="admin-page-header__actions">{actions}</div>}
    </header>
  )
}

export function AdminFilterBar({
  children,
  extra,
  className,
}: AdminFilterBarProps) {
  return (
    <section
      className={joinClassNames('admin-filter-bar', className)}
      aria-label="筛选条件"
    >
      <div className="admin-filter-bar__controls">{children}</div>
      {extra && <div className="admin-filter-bar__extra">{extra}</div>}
    </section>
  )
}

export function AdminContentCard({
  children,
  title,
  description,
  extra,
  flush = false,
  className,
}: AdminContentCardProps) {
  const hasHeader = Boolean(title || description || extra)

  return (
    <section className={joinClassNames('admin-content-card', className)}>
      {hasHeader && (
        <div className="admin-content-card__header">
          <div>
            {title && <h2>{title}</h2>}
            {description && <p>{description}</p>}
          </div>
          {extra && <div className="admin-content-card__extra">{extra}</div>}
        </div>
      )}
      <div
        className={joinClassNames(
          'admin-content-card__body',
          flush && 'admin-content-card__body--flush',
        )}
      >
        {children}
      </div>
    </section>
  )
}

export function AdminCount({ children }: AdminCountProps) {
  return <span className="admin-count">{children}</span>
}
