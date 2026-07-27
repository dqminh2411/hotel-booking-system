import styles from './Button.module.scss';

/**
 * Nut dung chung cho toan bo app - moi feature deu nen dung lai component nay.
 * Props: variant ('primary'|'secondary'|'danger'|'ghost'), size ('sm'|'md'|'lg'),
 * fullWidth, isLoading, leftIcon - cong voi moi thuoc tinh <button> chuan khac.
 */
export function Button({
  variant = 'primary',
  size = 'md',
  fullWidth,
  isLoading,
  leftIcon,
  className,
  children,
  disabled,
  ...rest
}) {
  const sizeClass = size === 'md' ? '' : styles[size];

  return (
    <button
      className={[styles.btn, styles[variant], sizeClass, fullWidth ? styles.fullWidth : '', className]
        .filter(Boolean)
        .join(' ')}
      disabled={disabled || isLoading}
      {...rest}
    >
      {isLoading ? 'Dang xu ly...' : (
        <>
          {leftIcon}
          {children}
        </>
      )}
    </button>
  );
}
