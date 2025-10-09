import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import logo from '@/assets/memories_logo.png';
import styles from './Navigation.module.css';
import SearchInput from '../SearchInput';

import screenshot from '@/assets/screenshot.svg';
import object from '@/assets/object.svg';
import notification from '@/assets/notification.svg';
import contact from '@/assets/contact.svg';
import logoutIcon from '@/assets/logout.svg';
import userProfile from '@/assets/user_profile.png';

function Navigation() {
  const navigate = useNavigate();
  const { user, isAuthenticated, logout } = useAuth();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const handleLogoClick = () => {
    navigate('/');
  };

  return (
    <div className={styles.navigation}>
      <img
        src={logo}
        alt="logo"
        className={styles.logo}
        onClick={handleLogoClick}
        style={{ cursor: 'pointer' }}
      />
      <div className={styles.searchContainer}>
        <SearchInput />
      </div>

      {/* 메뉴 항목들 */}
      <nav className={styles.menuItems}>
        <button
          type="button"
          className={styles.menuItem}
          onClick={() => navigate('/post')}
        >
          <img src={screenshot} alt="screenshot" />
          <span>장면</span>
        </button>
        <button
          type="button"
          className={styles.menuItem}
          onClick={() => navigate('/post')}
        >
          <img src={object} alt="object" />
          <span>물체</span>
        </button>
        <button
          type="button"
          className={styles.menuItem}
          onClick={() => alert('공지사항 기능 준비 중')}
        >
          <img src={notification} alt="notification" />
          <span>공지사항</span>
        </button>
        <button
          type="button"
          className={styles.menuItem}
          onClick={() => alert('문의하기 기능 준비 중')}
        >
          <img src={contact} alt="contact" />
          <span>문의하기</span>
        </button>

        {isAuthenticated ? (
          <div className={styles.logoutSection}>
            <button
              type="button"
              className={styles.logoutButton}
              onClick={handleLogout}
            >
              <img src={logoutIcon} alt="logout" />
              <span>Log Out</span>
            </button>
          </div>
        ) : (
          <div className={styles.logoutSection}>
            <button
              type="button"
              className={styles.logoutButton}
              onClick={() => navigate('/login')}
            >
              <span>Log In</span>
            </button>
          </div>
        )}
      </nav>

      <div
        className={styles.userInfo}
        onClick={() => {
          if (isAuthenticated) {
            navigate('/profile');
          } else {
            navigate('/login');
          }
        }}
        style={{ cursor: 'pointer' }}
      >
        <div className={styles.userAvatar}>
          <img src={userProfile} alt="userProfile" />
        </div>
        <div className={styles.userDetails}>
          {isAuthenticated && user ? (
            <>
              <div className={styles.userName}>{user.name || user.nickname}</div>
              <div className={styles.userEmail}>{user.email}</div>
            </>
          ) : (
            <>
              <div className={styles.userName}>게스트</div>
              <div className={styles.userEmail}>로그인이 필요합니다</div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default Navigation;
