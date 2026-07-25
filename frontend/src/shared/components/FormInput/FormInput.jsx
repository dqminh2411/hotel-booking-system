import { forwardRef } from 'react';
import styles from './FormInput.module.scss';

/** Input dung chung cho toan bo form, hien thi label + error thong nhat. */
export const FormInput = forwardRef(function FormInput(
  { label, error, helpText, required, id, className, ...rest },
  ref,
) {
  const inputId = id ?? rest.name;

  return (
    <div className={styles.field}>
      {label && (
        <label className={styles.label} htmlFor={inputId}>
          {label}
          {required && <span className={styles.required}>*</span>}
        </label>
      )}
      <input
        id={inputId}
        ref={ref}
        className={[styles.input, error ? styles.hasError : '', className].filter(Boolean).join(' ')}
        aria-invalid={!!error}
        {...rest}
      />
      {error ? (
        <span className={styles.errorText}>{error}</span>
      ) : (
        helpText && <span className={styles.helpText}>{helpText}</span>
      )}
    </div>
  );
});
