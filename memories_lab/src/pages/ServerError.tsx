import { useNavigate } from 'react-router-dom';
import { useMobile } from '@/hooks/useMobile';
import Navigation from '@/components/main/Navigation';
import MobileNavigation from '@/components/main/MobileNavigation';
import styles from './ServerError.module.css';

export default function ServerError() {
  const navigate = useNavigate();
  const { isMobile } = useMobile();

  const handleRefresh = () => {
    window.location.reload();
  };

  return (
    <div className={styles.container}>
      {isMobile ? <MobileNavigation /> : <Navigation />}

      <div className={styles.content}>
        <div className={styles.errorBox}>
          <h1 className={styles.errorCode}>500</h1>
          <h2 className={styles.errorTitle}>서버 오류가 발생했습니다</h2>
          <p className={styles.errorMessage}>
            일시적인 문제로 서비스를 사용할 수 없습니다.
            <br />
            잠시 후 다시 시도해주세요.
          </p>
          <div className={styles.actions}>
            <button
              type="button"
              onClick={handleRefresh}
              className={styles.secondaryButton}
            >
              새로고침
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
