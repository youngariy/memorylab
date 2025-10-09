import { Link, useLocation } from 'react-router-dom';
import styles from './Breadcrumbs.module.css';

interface BreadcrumbItem {
  label: string;
  path?: string;
}

interface BreadcrumbsProps {
  items: BreadcrumbItem[];
}

export default function Breadcrumbs({ items }: BreadcrumbsProps) {
  const location = useLocation();

  return (
    <nav className={styles.breadcrumbs} aria-label="breadcrumb">
      <ol className={styles.list}>
        {items.map((item, index) => {
          const isLast = index === items.length - 1;
          const isCurrent = item.path === location.pathname;

          return (
            <li key={index} className={styles.item}>
              {!isLast && item.path ? (
                <>
                  <Link
                    to={item.path}
                    className={`${styles.link} ${isCurrent ? styles.active : ''}`}
                  >
                    {item.label}
                  </Link>
                  <span className={styles.separator}>/</span>
                </>
              ) : (
                <span className={styles.current}>{item.label}</span>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
