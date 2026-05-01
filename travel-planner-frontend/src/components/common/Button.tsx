import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost'

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  loading?: boolean
  children: ReactNode
}

const variantClass: Record<Variant, string> = {
  primary: 'bg-blue-600 text-white hover:bg-blue-700 disabled:bg-blue-300',
  secondary: 'bg-gray-100 text-gray-700 hover:bg-gray-200 disabled:opacity-50',
  danger: 'bg-red-500 text-white hover:bg-red-600 disabled:bg-red-300',
  ghost: 'text-blue-600 hover:bg-blue-50 disabled:opacity-50',
}

export default function Button({ variant = 'primary', loading, children, className = '', ...rest }: Props) {
  return (
    <button
      disabled={loading || rest.disabled}
      className={`inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium
        transition-colors focus:outline-none focus:ring-2 focus:ring-blue-400
        ${variantClass[variant]} ${className}`}
      {...rest}
    >
      {loading && (
        <span className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
      )}
      {children}
    </button>
  )
}
