import { useState } from 'react';

export default function FormField({
  id,
  label,
  error,
  type = 'text',
  autoComplete,
  ...inputProps
}) {
  const [passwordVisible, setPasswordVisible] = useState(false);
  const isPassword = type === 'password';
  const resolvedType = isPassword && passwordVisible ? 'text' : type;

  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-slate-700">
        {label}
      </label>
      <div className="relative">
        <input
          id={id}
          type={resolvedType}
          autoComplete={autoComplete}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${id}-error` : undefined}
          className={`form-input ${error ? 'border-red-500 focus:border-red-600 focus:ring-red-100' : ''} ${
            isPassword ? 'pr-16' : ''
          }`}
          {...inputProps}
        />
        {isPassword && (
          <button
            type="button"
            onClick={() => setPasswordVisible((visible) => !visible)}
            className="absolute inset-y-0 right-0 px-3 text-xs font-semibold text-blue-700 hover:text-blue-800"
            aria-label={passwordVisible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
          >
            {passwordVisible ? 'Ẩn' : 'Hiện'}
          </button>
        )}
      </div>
      {error && (
        <p id={`${id}-error`} className="mt-1 text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}
