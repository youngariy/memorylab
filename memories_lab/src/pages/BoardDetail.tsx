import { useParams, useNavigate } from 'react-router-dom';
import MobileNavigation from '@/components/main/MobileNavigation';
import Navigation from '@/components/main/Navigation';
import Breadcrumbs from '@/components/common/Breadcrumbs';
import BoardDetailContent from '@/components/Post/BoardDetailContent';
import { useMobile } from '@/hooks/useMobile';
import styles from './BoardDetail.module.css';

function BoardDetail() {
  const { isMobile } = useMobile();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const breadcrumbs = [
    { label: '홈', path: '/' },
    { label: '게시글', path: '/post' },
    { label: `게시글 #${id}` },
  ];

  const handleBackClick = () => {
    navigate('/post');
  };

  return (
    <div className={styles.boardDetailContainer}>
      {isMobile ? <MobileNavigation /> : <Navigation />}
      <div className={styles.boardDetailContent}>
        <div className={styles.headerSection}>
          <button
            onClick={handleBackClick}
            className={styles.backButton}
            aria-label="게시글 목록으로 돌아가기"
          >
            ← 목록으로
          </button>
          <Breadcrumbs items={breadcrumbs} />
        </div>
        <BoardDetailContent />
      </div>
    </div>
  );
}

export default BoardDetail;
