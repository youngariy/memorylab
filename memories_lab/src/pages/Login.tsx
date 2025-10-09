import { useState, FormEvent, useEffect } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import Navigation from '../components/main/Navigation';
import MobileNavigation from '../components/main/MobileNavigation';
import { useMobile } from '../hooks/useMobile';
import styles from './Login.module.css';

function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [redirectMessage, setRedirectMessage] = useState('');

  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const { isMobile } = useMobile();
  const [searchParams] = useSearchParams();

  const redirectTo = searchParams.get('redirect') || '/';

  useEffect(() => {
    // If already logged in, redirect immediately
    if (isAuthenticated) {
      navigate(redirectTo, { replace: true });
    }

    // Show contextual message if user was redirected from protected route
    if (redirectTo !== '/') {
      setRedirectMessage('로그인이 필요합니다. 로그인 후 원하는 페이지로 이동합니다.');
    }
  }, [isAuthenticated, navigate, redirectTo]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await login({ email, password });
      // Navigate to redirect URL after successful login
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : '로그인에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className={styles.loginContainer}>
      {isMobile ? <MobileNavigation /> : <Navigation />}

      <div className={styles.loginContent}>
        <div className={styles.loginBox}>
          <h1 className={styles.title}>로그인</h1>

          {redirectMessage && (
            <div className={styles.info}>
              <p>{redirectMessage}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className={styles.form}>
            {error && <div className={styles.error}>{error}</div>}

            <div className={styles.formGroup}>
              <label htmlFor="email">이메일</label>
              <input
                type="email"
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                placeholder="이메일을 입력하세요"
              />
            </div>

            <div className={styles.formGroup}>
              <label htmlFor="password">비밀번호</label>
              <input
                type="password"
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
                placeholder="비밀번호를 입력하세요"
              />
            </div>

            <button
              type="submit"
              className={styles.submitButton}
              disabled={isLoading}
            >
              {isLoading ? '로그인 중...' : '로그인'}
            </button>
          </form>

          <div className={styles.links}>
            <span>계정이 없으신가요? </span>
            <Link to="/register">회원가입</Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;
