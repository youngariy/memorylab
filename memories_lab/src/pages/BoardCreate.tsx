import MobileNavigation from '@/components/main/MobileNavigation';
import Navigation from '@/components/main/Navigation';
import Breadcrumbs from '@/components/common/Breadcrumbs';
import styles from './BoardCreate.module.css';
import BoardCreateContent from '@/components/Post/BoardCreateContent';
import { useMobile } from '@/hooks/useMobile';

function BoardCreate() {
  const { isMobile } = useMobile();

  const breadcrumbs = [
    { label: '홈', path: '/' },
    { label: '게시글', path: '/post' },
    { label: '새 글 작성' },
  ];

  return (
    <div className={styles.boardCreateContainer}>
      {isMobile ? <MobileNavigation /> : <Navigation />}
      <div className={styles.boardCreateContent}>
        <Breadcrumbs items={breadcrumbs} />
        <BoardCreateContent />
      </div>
    </div>
  );
}

export default BoardCreate;
