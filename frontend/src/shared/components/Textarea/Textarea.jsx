import { forwardRef } from 'react';
import formStyles from '@/shared/components/FormInput/FormInput.module.scss';

export const Textarea = forwardRef(function Textarea(
  { label, error, helpText, required, id, className, rows = 3, ...rest },
  ref,
) {
  const inputId = id ?? rest.name;

  return (
    <div className={formStyles.field}>
      {label && (
        <label className={formStyles.label} htmlFor={inputId}>
          {label}
          {required && <span className={formStyles.required}>*</span>}
        </label>
      )}
      <textarea
        id={inputId}
        ref={ref}
        rows={rows}
        className={[formStyles.input, error ? formStyles.hasError : '', className].filter(Boolean).join(' ')}
        style={{ height: 'auto', padding: '8px 12px', resize: 'vertical' }}
        aria-invalid={!!error}
        {...rest}
      />
      {error ? (
        <span className={formStyles.errorText}>{error}</span>
      ) : (
        helpText && <span className={formStyles.helpText}>{helpText}</span>
      )}
    </div>
  );
});
