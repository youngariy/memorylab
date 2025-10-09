import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authEndpoints, boardEndpoints } from '@/services/endpoints';
import { useAuth } from '@/hooks/useAuth';
import type { User, BoardSummary } from '@/types/api';
import { categoryToLabel } from '@/types/api';
import Navigation from '@/components/main/Navigation';
import MobileNavigation from '@/components/main/MobileNavigation';
import Breadcrumbs from '@/components/common/Breadcrumbs';
import { useMobile } from '@/hooks/useMobile';
import styles from './Profile.module.css';

function Profile() {
  const navigate = useNavigate();
  const { user: authUser, isAuthenticated, isAdmin } = useAuth();
  const { isMobile } = useMobile();

  const [user, setUser] = useState<User | null>(null);
  const [myPosts, setMyPosts] = useState<BoardSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingPosts, setIsLoadingPosts] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [totalPosts, setTotalPosts] = useState(0);

  const breadcrumbs = [
    { label: '홈', path: '/' },
    { label: '내 정보' },
  ];

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login?redirect=/profile');
      return;
    }

    loadProfile();
    loadMyPosts();
  }, [isAuthenticated, navigate]);

  const loadProfile = async () => {
    try {
      const userData = await authEndpoints.me();
      setUser(userData);
    } catch (err) {
      console.error('Failed to load profile:', err);
      setError('프로필을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const loadMyPosts = async () => {
    try {
      const response = await boardEndpoints.myBoards({ page: 0, size: 10 });
      setMyPosts(response.content);
      setTotalPosts(response.totalElements);
    } catch (err) {
      console.error('Failed to load my posts:', err);
    } finally {
      setIsLoadingPosts(false);
    }
  };

  const formatDate = (dateString: string | null) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (isLoading) {
    return (
      <div className={styles.profileContainer}>
        {isMobile ? <MobileNavigation /> : <Navigation />}
        <div className={styles.profileContent}>
          <div className={styles.loadingContainer}>
            <div className={styles.spinner}></div>
            <p>프로필을 불러오는 중...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error || !user) {
    return (
      <div className={styles.profileContainer}>
        {isMobile ? <MobileNavigation /> : <Navigation />}
        <div className={styles.profileContent}>
          <div className={styles.errorContainer}>
            <p>{error || '프로필을 불러올 수 없습니다.'}</p>
            <button onClick={() => navigate('/')} className={styles.backButton}>
              홈으로 돌아가기
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.profileContainer}>
      {isMobile ? <MobileNavigation /> : <Navigation />}
      <div className={styles.profileContent}>
        <Breadcrumbs items={breadcrumbs} />

        <h1 className={styles.pageTitle}>내 정보</h1>

        {/* Profile Card */}
        <section className={styles.profileCard}>
          <div className={styles.profileRow}>
            <div className={styles.label}>닉네임</div>
            <div className={styles.value}>
              {user.nickname}
              {isAdmin && <span className={styles.adminBadge}>👑 관리자</span>}
            </div>
          </div>
          <div className={styles.profileRow}>
            <div className={styles.label}>이름</div>
            <div className={styles.value}>{user.name || '-'}</div>
          </div>
          <div className={styles.profileRow}>
            <div className={styles.label}>이메일</div>
            <div className={styles.value}>{user.email}</div>
          </div>
          <div className={styles.profileRow}>
            <div className={styles.label}>가입일</div>
            <div className={styles.value}>{formatDate(user.createdAt)}</div>
          </div>
        </section>

        {/* My Posts Section */}
        <h2 className={styles.sectionTitle}>
          내가 쓴 글 <span className={styles.postCount}>({totalPosts})</span>
        </h2>

        <section className={styles.postsCard}>
          {isLoadingPosts ? (
            <div className={styles.loadingText}>게시글을 불러오는 중...</div>
          ) : myPosts.length === 0 ? (
            <div className={styles.emptyText}>작성한 게시글이 없습니다.</div>
          ) : (
            <div className={styles.tableWrapper}>
              <table className={styles.postsTable}>
                <thead>
                  <tr>
                    <th className={styles.colId}>번호</th>
                    <th className={styles.colTitle}>제목</th>
                    <th className={styles.colCategory}>카테고리</th>
                    <th className={styles.colViews}>조회</th>
                    <th className={styles.colDate}>작성일</th>
                  </tr>
                </thead>
                <tbody>
                  {myPosts.map((post, index) => (
                    <tr
                      key={post.id}
                      className={styles.postRow}
                      onClick={() => navigate(`/post/${post.id}`)}
                    >
                      <td>{totalPosts - index}</td>
                      <td className={styles.titleCell}>
                        <div className={styles.titleLine}>
                          <span className={styles.titleText}>{post.title}</span>
                          {post.status === 'READY' && (
                            <span className={styles.icon}>🎥</span>
                          )}
                          {(post.status === 'PROCESSING' || post.status === 'DISPATCHED') && (
                            <span className={styles.icon}>⏳</span>
                          )}
                          {post.visibility === 'PRIVATE' && (
                            <span className={styles.privateBadge}>나만보기</span>
                          )}
                        </div>
                      </td>
                      <td>{categoryToLabel(post.category)}</td>
                      <td>{post.viewCount}</td>
                      <td>{formatDate(post.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

export default Profile;
