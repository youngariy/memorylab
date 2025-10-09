import { useNavigate } from 'react-router-dom';
import styles from './Header.module.css';
import logo from '@/assets/memories_logo.png';

function Header() {
  const navigate = useNavigate();

  const handleLogoClick = () => {
    navigate('/');
  };

  const handleStartClick = () => {
    navigate('/post');
  };

  return (
    <header className={styles.header}>
      <div className={styles.container}>
        <div className={styles.logo}>
          <img
            src={logo}
            alt="logo"
            onClick={handleLogoClick}
            style={{ cursor: 'pointer' }}
          />
        </div>

        <div className={styles.rightSection}>
          <button
            type="button"
            className={styles.applyButton}
            onClick={handleStartClick}
          >
            시작하기
          </button>
        </div>
      </div>
    </header>
  );
}

export default Header;
