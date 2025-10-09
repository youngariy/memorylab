import { useNavigate } from 'react-router-dom';
import { useMobile } from '@/hooks/useMobile';
import Navigation from '@/components/main/Navigation';
import MobileNavigation from '@/components/main/MobileNavigation';
import styles from './NotFound.module.css';

export default function NotFound() {
  const navigate = useNavigate();
  const { isMobile } = useMobile();

  return (
    <div className={styles.container}>
      {isMobile ? <MobileNavigation /> : <Navigation />}

      <div className={styles.content}>
        <div className={styles.errorBox}>
          <h1 className={styles.errorCode}>404</h1>
          <h2 className={styles.errorTitle}>페이지를 찾을 수 없습니다</h2>
          <p className={styles.errorMessage}>
            요청하신 페이지가 존재하지 않거나 이동되었습니다.
          </p>
          <div className={styles.actions}>
            <button
              type="button"
              onClick={() => navigate(-1)}
              className={styles.secondaryButton}
            >
              이전 페이지
            </button>
            <button
              type="button"
              onClick={() => navigate('/')}
              className={styles.primaryButton}
            >
              홈으로 돌아가기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
