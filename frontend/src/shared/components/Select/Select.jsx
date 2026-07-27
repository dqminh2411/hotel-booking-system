import { forwardRef } from 'react';
import formStyles from '@/shared/components/FormInput/FormInput.module.scss';

/**
 * Select dung chung, nhan mang options {value,label} de tai su dung cho moi enum.
 * @param {Object} props
 * @param {Array<{value: string, label: string}>} props.options
 */
export const Select = forwardRef(function Select(
  { label, error, helpText, required, id, className, options, placeholder, ...rest },
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
      <select
        id={inputId}
        ref={ref}
        className={[formStyles.input, error ? formStyles.hasError : '', className].filter(Boolean).join(' ')}
        aria-invalid={!!error}
        {...rest}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
      {error ? (
        <span className={formStyles.errorText}>{error}</span>
      ) : (
        helpText && <span className={formStyles.helpText}>{helpText}</span>
      )}
    </div>
  );
});
