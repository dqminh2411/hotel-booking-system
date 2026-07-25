import styles from './Table.module.scss';
import { Loading } from '@/shared/components/Loading/Loading';
import { EmptyState } from '@/shared/components/EmptyState/EmptyState';

/**
 * Bang du lieu dung chung, nhan columns tuong tu Ant Design/MUI nhung
 * khong phu thuoc UI library nao - de cac feature khac (coupon, hotel...) tai su dung.
 *
 * @param {Object} props
 * @param {Array<{key: string, header: string, align?: 'left'|'right', width?: string, render: (row: any) => any}>} props.columns
 * @param {Array} props.data
 * @param {(row: any) => string} props.rowKey
 * @param {boolean} [props.isLoading]
 * @param {string} [props.emptyMessage]
 */
export function Table({ columns, data, rowKey, isLoading, emptyMessage }) {
  return (
    <div className={styles.wrapper}>
      <div className={styles.scroller}>
        <table className={styles.table}>
          <thead>
            <tr>
              {columns.map((col) => (
                <th
                  key={col.key}
                  className={col.align === 'right' ? styles.alignRight : undefined}
                  style={col.width ? { width: col.width } : undefined}
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr className={styles.loadingRow}>
                <td colSpan={columns.length}>
                  <Loading />
                </td>
              </tr>
            ) : data.length === 0 ? (
              <tr className={styles.emptyRow}>
                <td colSpan={columns.length}>
                  <EmptyState message={emptyMessage ?? 'Khong co du lieu'} />
                </td>
              </tr>
            ) : (
              data.map((row) => (
                <tr key={rowKey(row)}>
                  {columns.map((col) => (
                    <td key={col.key} className={col.align === 'right' ? styles.alignRight : undefined}>
                      {col.render(row)}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
