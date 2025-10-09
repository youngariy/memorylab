import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import styles from './PostNavigation.module.css';
import speechIcon from '@/assets/speech.svg';
import refreshIcon from '@/assets/refresh.svg';

interface PostNavigationProps {
  totalCount: number;
  searchQuery: string;
  onSearchChange: (query: string) => void;
}

export default function PostNavigation({ totalCount, searchQuery, onSearchChange }: PostNavigationProps) {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const handleCreateClick = () => {
    if (isAuthenticated) {
      navigate('/post/create');
    } else {
      navigate('/login?redirect=/post/create');
    }
  };

  return (
    <div className={styles.postNavigation}>
      <div className={styles.postNavigationLeft}>
        <img src={speechIcon} alt="speech" />
        <span className={styles.postNavigationLeftTitle}>게시글 {totalCount}</span>
        <img src={refreshIcon} alt="refresh" />
      </div>

      <div className={styles.postNavigationRight}>
        <input
          type="text"
          placeholder="검색..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          className={styles.searchInput}
        />
        <button type="button" className={styles.createButton} onClick={handleCreateClick}>
          새글 작성
        </button>
      </div>
    </div>
  );
}
