import { useState, FormEvent, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import logo from '@/assets/memories_logo.png';
import styles from './Register.module.css';

type Step = 'email' | 'verify' | 'details';

function Register() {
  const [step, setStep] = useState<Step>('email');
  const [email, setEmail] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [name, setName] = useState('');
  const [nickname, setNickname] = useState('');

  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const [emailValid, setEmailValid] = useState(false);
  const [emailChecking, setEmailChecking] = useState(false);
  const [nicknameValid, setNicknameValid] = useState(false);
  const [nicknameChecked, setNicknameChecked] = useState(false);

  const { sendVerificationCode, verifyCode, register, checkEmailAvailability, checkNicknameAvailability } = useAuth();
  const navigate = useNavigate();

  // Email validation check with proper debouncing and error handling
  useEffect(() => {
    const checkEmail = async () => {
      // Reset validation state if email is empty or invalid format
      if (!email || !email.trim()) {
        setEmailValid(false);
        setEmailChecking(false);
        return;
      }

      // Basic email format validation
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(email)) {
        setEmailValid(false);
        setEmailChecking(false);
        return;
      }

      setEmailChecking(true);
      try {
        const available = await checkEmailAvailability(email);
        setEmailValid(available);
        if (!available) {
          setError('이미 사용 중인 이메일입니다.');
        } else {
          setError('');
        }
      } catch (err) {
        setEmailValid(false);
        setError('이메일 확인 중 오류가 발생했습니다.');
      } finally {
        setEmailChecking(false);
      }
    };

    const timeoutId = setTimeout(checkEmail, 500);
    return () => clearTimeout(timeoutId);
  }, [email, checkEmailAvailability]);

  // Nickname validation check
  const handleNicknameCheck = async () => {
    if (!nickname) {
      setError('닉네임을 입력하세요.');
      return;
    }

    try {
      const available = await checkNicknameAvailability(nickname);
      setNicknameValid(available);
      setNicknameChecked(true);
      if (available) {
        setSuccess('사용 가능한 닉네임입니다.');
        setError('');
      } else {
        setError('이미 사용 중인 닉네임입니다.');
        setSuccess('');
      }
    } catch (err) {
      setError('닉네임 확인에 실패했습니다.');
    }
  };

  const handleSendCode = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setIsLoading(true);

    if (!emailValid) {
      setError('유효한 이메일을 입력하세요.');
      setIsLoading(false);
      return;
    }

    try {
      await sendVerificationCode({ email });
      setSuccess('인증 코드가 이메일로 전송되었습니다.');
      setStep('verify');
    } catch (err) {
      setError(err instanceof Error ? err.message : '인증 코드 전송에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleVerifyCode = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setIsLoading(true);

    try {
      await verifyCode({ email, code: verificationCode });
      setSuccess('이메일이 인증되었습니다.');
      setStep('details');
    } catch (err) {
      setError(err instanceof Error ? err.message : '인증 코드가 올바르지 않습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegister = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (password !== confirmPassword) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    if (!nicknameChecked || !nicknameValid) {
      setError('닉네임 중복 확인을 해주세요.');
      return;
    }

    setIsLoading(true);

    try {
      await register({ email, password, name, nickname });
      setSuccess('회원가입이 완료되었습니다. 로그인해주세요.');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      setError(err instanceof Error ? err.message : '회원가입에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className={styles.registerContainer}>
      {/* Simple header with logo */}
      <div className={styles.header}>
        <Link to="/" className={styles.logoLink}>
          <img src={logo} alt="Memories Lab" className={styles.logo} />
        </Link>
      </div>

      <div className={styles.registerContent}>
        <div className={styles.registerBox}>
          <h1 className={styles.title}>회원가입</h1>
          <p className={styles.subtitle}>추억현상소에서 새로운 시작을 함께하세요</p>

          <div className={styles.stepIndicator}>
            <div className={`${styles.stepItem} ${step === 'email' ? styles.active : ''} ${step !== 'email' ? styles.completed : ''}`}>
              <div className={styles.stepNumber}>1</div>
              <div className={styles.stepLabel}>이메일</div>
            </div>
            <div className={`${styles.stepItem} ${step === 'verify' ? styles.active : ''} ${step === 'details' ? styles.completed : ''}`}>
              <div className={styles.stepNumber}>2</div>
              <div className={styles.stepLabel}>인증</div>
            </div>
            <div className={`${styles.stepItem} ${step === 'details' ? styles.active : ''}`}>
              <div className={styles.stepNumber}>3</div>
              <div className={styles.stepLabel}>정보입력</div>
            </div>
          </div>

          {error && <div className={styles.error}>{error}</div>}
          {success && <div className={styles.success}>{success}</div>}

          {step === 'email' && (
            <form onSubmit={handleSendCode} className={styles.form}>
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
                {emailChecking && <span className={styles.checking}>확인 중...</span>}
                {!emailChecking && email && emailValid && <span className={styles.valid}>✓ 사용 가능한 이메일입니다.</span>}
              </div>

              <button type="submit" className={styles.submitButton} disabled={isLoading || !emailValid || emailChecking}>
                {isLoading ? '전송 중...' : '인증 코드 받기'}
              </button>
            </form>
          )}

          {step === 'verify' && (
            <form onSubmit={handleVerifyCode} className={styles.form}>
              <div className={styles.formGroup}>
                <label htmlFor="code">인증 코드</label>
                <input
                  type="text"
                  id="code"
                  value={verificationCode}
                  onChange={(e) => setVerificationCode(e.target.value)}
                  required
                  placeholder="이메일로 받은 인증 코드를 입력하세요"
                  maxLength={6}
                />
              </div>

              <button type="submit" className={styles.submitButton} disabled={isLoading}>
                {isLoading ? '확인 중...' : '인증하기'}
              </button>

              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => setStep('email')}
              >
                이메일 변경
              </button>
            </form>
          )}

          {step === 'details' && (
            <form onSubmit={handleRegister} className={styles.form}>
              <div className={styles.formGroup}>
                <label htmlFor="name">이름</label>
                <input
                  type="text"
                  id="name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  placeholder="이름을 입력하세요"
                />
              </div>

              <div className={styles.formGroup}>
                <label htmlFor="nickname">닉네임</label>
                <div className={styles.nicknameGroup}>
                  <input
                    type="text"
                    id="nickname"
                    value={nickname}
                    onChange={(e) => {
                      setNickname(e.target.value);
                      setNicknameChecked(false);
                    }}
                    required
                    placeholder="닉네임을 입력하세요"
                  />
                  <button
                    type="button"
                    className={styles.checkButton}
                    onClick={handleNicknameCheck}
                  >
                    중복확인
                  </button>
                </div>
                {nicknameChecked && nicknameValid && (
                  <span className={styles.valid}>✓ 사용 가능한 닉네임입니다.</span>
                )}
              </div>

              <div className={styles.formGroup}>
                <label htmlFor="password">비밀번호</label>
                <input
                  type="password"
                  id="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete="new-password"
                  placeholder="비밀번호를 입력하세요"
                  minLength={8}
                />
              </div>

              <div className={styles.formGroup}>
                <label htmlFor="confirmPassword">비밀번호 확인</label>
                <input
                  type="password"
                  id="confirmPassword"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  autoComplete="new-password"
                  placeholder="비밀번호를 다시 입력하세요"
                  minLength={8}
                />
              </div>

              <button type="submit" className={styles.submitButton} disabled={isLoading}>
                {isLoading ? '가입 중...' : '회원가입'}
              </button>
            </form>
          )}

          <div className={styles.links}>
            <span>이미 계정이 있으신가요? </span>
            <Link to="/login">로그인</Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Register;
