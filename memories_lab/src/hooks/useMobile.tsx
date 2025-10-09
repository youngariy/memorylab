import { useEffect, useState } from 'react';

export const useMobile = () => {
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    const checkScreenSize = () => {
      setIsMobile(window.innerWidth <= 768);
    };

    // 초기 화면 크기 확인
    checkScreenSize();

    // 윈도우 리사이즈 이벤트 리스너 추가
    window.addEventListener('resize', checkScreenSize);

    // 컴포넌트 언마운트 시 이벤트 리스너 제거
    return () => {
      window.removeEventListener('resize', checkScreenSize);
    };
  }, []);

  return { isMobile, setIsMobile };
};
