import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * Scrolls to top on route change and restores scroll position on back navigation
 */
export default function ScrollToTop() {
  const { pathname } = useLocation();

  useEffect(() => {
    // Scroll to top when pathname changes
    window.scrollTo({
      top: 0,
      left: 0,
      behavior: 'instant' as ScrollBehavior,
    });
  }, [pathname]);

  return null;
}
