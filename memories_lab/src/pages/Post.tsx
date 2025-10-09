import { useState } from 'react';
import styles from './Post.module.css';
import Navigation from '@/components/main/Navigation';
import MobileNavigation from '@/components/main/MobileNavigation';
import PostNavigation from '@/components/Post/PostNavigation';
import BoardList from '@/components/Post/BoardList';
import { useMobile } from '@/hooks/useMobile';

function Post() {
  const { isMobile } = useMobile();
  const [totalCount, setTotalCount] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');

  return (
    <div className={styles.postContainer}>
      {isMobile ? <MobileNavigation /> : <Navigation />}
      <div className={styles.postContent}>
        <PostNavigation
          totalCount={totalCount}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
        />
        <BoardList
          searchQuery={searchQuery}
          onTotalCountChange={setTotalCount}
        />
      </div>
    </div>
  );
}

export default Post;
