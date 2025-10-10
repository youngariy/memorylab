import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuth } from '@/hooks/useAuth';
import styles from './MobileNavigation.module.css';
import logo from '@/assets/memories_logo.png';
import menuIcon from '@/assets/menu_line.svg';
import userProfile from '@/assets/user_profile.png';
import screenshot from '@/assets/screenshot.svg';
import notification from '@/assets/notification.svg';
import contact from '@/assets/contact.svg';
import object from '@/assets/object.svg';
import logoutIcon from '@/assets/logout.svg';
import SearchInput from '../SearchInput';

function MobileNavigation() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const navigate = useNavigate();
  const { user, isAuthenticated, logout } = useAuth();

  const toggleMenu = () => {
    setIsMenuOpen(!isMenuOpen);
  };

  const closeMenu = () => {
    setIsMenuOpen(false);
  };

  const handleNavigation = (path: string) => {
    closeMenu();
    navigate(path);
  };

  const handleLogout = () => {
    logout();
    closeMenu();
    navigate('/');
  };

  const handleLogoClick = () => {
    navigate('/');
  };

  const handleSearch = (searchQuery: string) => {
    if (searchQuery.trim()) {
      navigate(`/post?search=${encodeURIComponent(searchQuery.trim())}`);
    } else {
      navigate('/post');
    }
  };

  return (
    <>
      <div className={styles.navigation}>
        <div className={styles.topNavigtion}>
          <img
            src={logo}
            alt="logo"
            onClick={handleLogoClick}
            style={{ cursor: 'pointer' }}
          />
          <button
            type="button"
            onClick={toggleMenu}
            className={styles.menuIconButton}
            aria-label="메뉴 열기"
          >
            <img src={menuIcon} alt="menu" className={styles.menuIcon} />
          </button>
        </div>
        <div className={styles.searchContainer}>
          <SearchInput onSearch={handleSearch} />
        </div>
      </div>

      <AnimatePresence>
        {isMenuOpen && (
          <>
            {/* 배경 오버레이 */}
            <motion.div
              className={styles.overlay}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={closeMenu}
            />

            {/* 사이드바 */}
            <motion.div
              className={styles.sidebar}
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'tween', duration: 0.3 }}
            >
              <div className={styles.sidebarContent}>
                {/* 유저 정보 */}
                <div
                  className={styles.userInfo}
                  onClick={() => {
                    if (isAuthenticated) {
                      handleNavigation('/profile');
                    } else {
                      handleNavigation('/login');
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

                {/* 메뉴 항목들 */}
                <nav className={styles.menuItems}>
                  <button
                    type="button"
                    className={styles.menuItem}
                    onClick={() => handleNavigation('/post?category=SCENE')}
                  >
                    <img src={screenshot} alt="screenshot" />
                    <span>장면</span>
                  </button>
                  <button
                    type="button"
                    className={styles.menuItem}
                    onClick={() => handleNavigation('/post?category=OBJECT')}
                  >
                    <img src={object} alt="object" />
                    <span>물체</span>
                  </button>
                  <button
                    type="button"
                    className={styles.menuItem}
                    onClick={() => handleNavigation('/post?category=NOTICE')}
                  >
                    <img src={notification} alt="notification" />
                    <span>공지사항</span>
                  </button>
                  <button
                    type="button"
                    className={styles.menuItem}
                    onClick={() => handleNavigation('/post?category=QNA')}
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
                        onClick={() => handleNavigation('/login')}
                      >
                        <span>Log In</span>
                      </button>
                    </div>
                  )}
                </nav>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </>
  );
}

export default MobileNavigation;
