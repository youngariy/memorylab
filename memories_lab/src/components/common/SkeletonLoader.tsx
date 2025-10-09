import styles from './SkeletonLoader.module.css';

interface SkeletonLoaderProps {
  variant?: 'text' | 'rect' | 'circle';
  width?: string | number;
  height?: string | number;
  className?: string;
}

export default function SkeletonLoader({
  variant = 'text',
  width,
  height,
  className = '',
}: SkeletonLoaderProps) {
  const style = {
    width: typeof width === 'number' ? `${width}px` : width,
    height: typeof height === 'number' ? `${height}px` : height,
  };

  return (
    <div
      className={`${styles.skeleton} ${styles[variant]} ${className}`}
      style={style}
    />
  );
}

// Preset skeleton components for common patterns
export function BoardCardSkeleton() {
  return (
    <div className={styles.boardCardSkeleton}>
      <SkeletonLoader variant="rect" height={200} />
      <div className={styles.cardContent}>
        <SkeletonLoader variant="text" width="70%" height={24} />
        <SkeletonLoader variant="text" width="40%" height={16} />
        <div className={styles.cardFooter}>
          <SkeletonLoader variant="text" width="30%" height={14} />
          <SkeletonLoader variant="text" width="30%" height={14} />
        </div>
      </div>
    </div>
  );
}

export function CommentSkeleton() {
  return (
    <div className={styles.commentSkeleton}>
      <div className={styles.commentHeader}>
        <SkeletonLoader variant="circle" width={40} height={40} />
        <SkeletonLoader variant="text" width="120px" height={16} />
      </div>
      <SkeletonLoader variant="text" width="100%" height={16} />
      <SkeletonLoader variant="text" width="80%" height={16} />
    </div>
  );
}

export function BoardDetailSkeleton() {
  return (
    <div className={styles.boardDetailSkeleton}>
      <SkeletonLoader variant="text" width="60%" height={32} />
      <div className={styles.metaInfo}>
        <SkeletonLoader variant="text" width="100px" height={14} />
        <SkeletonLoader variant="text" width="100px" height={14} />
        <SkeletonLoader variant="text" width="100px" height={14} />
      </div>
      <SkeletonLoader variant="rect" height={300} />
      <SkeletonLoader variant="text" width="100%" height={16} />
      <SkeletonLoader variant="text" width="100%" height={16} />
      <SkeletonLoader variant="text" width="90%" height={16} />
    </div>
  );
}
