import { useNavigate } from 'react-router-dom';
import styles from './CTA.module.css';

function CTA() {
  const navigate = useNavigate();

  const handleStartClick = () => {
    navigate('/post');
  };

  return (
    <section className={styles.cta}>
      <div className={styles.backgroundImage} />

      <div className={styles.content}>
        <h2 className={styles.title}>
          기억을 현실로 만드는 마법,
          <br />
          추억현상소
        </h2>
        <p className={styles.description}>
          AI가 담아내는 입체적 경험으로, <br className={styles.mobileBr} />
          당신만의 이야기를 특별한 3D 세상에서 만나보세요.
        </p>
        <button
          type="button"
          className={styles.detailButton}
          onClick={handleStartClick}
        >
          시작하기
        </button>
      </div>
    </section>
  );
}

export default CTA;
