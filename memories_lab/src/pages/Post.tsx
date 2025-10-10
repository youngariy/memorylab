import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import styles from './Post.module.css';
import Navigation from '@/components/main/Navigation';
import MobileNavigation from '@/components/main/MobileNavigation';
import PostNavigation from '@/components/Post/PostNavigation';
import BoardList from '@/components/Post/BoardList';
import { useMobile } from '@/hooks/useMobile';

function Post() {
  const { isMobile } = useMobile();
  const [searchParams] = useSearchParams();
  const [totalCount, setTotalCount] = useState(0);
  const [category, setCategory] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Read category and search from URL query params
  useEffect(() => {
    const categoryParam = searchParams.get('category');
    const searchParam = searchParams.get('search');

    setCategory(categoryParam || '');
    setSearchQuery(searchParam || '');
  }, [searchParams]);

  return (
    <>
      {isMobile ? <MobileNavigation /> : <Navigation />}
      <div className={styles.postContainer}>
        <div className={styles.postContent}>
          <PostNavigation
            totalCount={totalCount}
          />
          <BoardList
            category={category}
            searchQuery={searchQuery}
            onTotalCountChange={setTotalCount}
          />
        </div>
      </div>
    </>
  );
}

export default Post;
