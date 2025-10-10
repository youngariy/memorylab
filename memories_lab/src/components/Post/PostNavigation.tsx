import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import styles from './PostNavigation.module.css';
import speechIcon from '@/assets/speech.svg';
import refreshIcon from '@/assets/refresh.svg';

interface PostNavigationProps {
  totalCount: number;
}

export default function PostNavigation({
  totalCount
}: PostNavigationProps) {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const handleCreateClick = () => {
    if (isAuthenticated) {
      navigate('/post/create');
    } else {
      navigate('/login?redirect=/post/create');
    }
  };

  const handleRefresh = () => {
    // URL 파라미터를 제거하고 /post로 이동하여 모든 카테고리 글 표시
    window.location.href = '/post';
  };

  return (
    <div className={styles.postNavigation}>
      <div className={styles.postNavigationLeft}>
        <img src={speechIcon} alt="speech" className={styles.icon} />
        <span className={styles.postNavigationLeftTitle}>전체 게시글</span>
        <span className={styles.countBadge}>{totalCount}</span>
        <button
          type="button"
          className={styles.refreshButton}
          onClick={handleRefresh}
          aria-label="새로고침"
        >
          <img src={refreshIcon} alt="refresh" className={styles.refreshIcon} />
        </button>
      </div>

      <div className={styles.postNavigationRight}>
        <button type="button" className={styles.createButton} onClick={handleCreateClick}>
          새글 작성
        </button>
      </div>
    </div>
  );
}
