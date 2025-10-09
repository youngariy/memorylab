import { useNavigate } from 'react-router-dom';
import type { BoardSummary } from '@/types/api';
import { categoryToLabel } from '@/types/api';
import styles from './Board.module.css';
import eyeIcon from '@/assets/eye.svg';
import commentIcon from '@/assets/comment.svg';
import thumbIcon from '@/assets/thumb.svg';

interface BoardProps {
  board: BoardSummary;
}

export default function Board({ board }: BoardProps) {
  const navigate = useNavigate();

  const handleClick = () => {
    navigate(`/post/${board.id}`);
  };

  // Format date
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
  };

  // Truncate content for preview
  const truncateContent = (text: string, maxLength: number = 100) => {
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  };

  // Handle thumbnail URL - ensure it's absolute or properly prefixed
  const getThumbnailUrl = (path: string | null): string | null => {
    if (!path) return null;
    // If path starts with http/https, use as-is
    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }
    // If path starts with /, it's relative to domain - use as-is for API proxy
    if (path.startsWith('/')) {
      return path;
    }
    // Otherwise, prefix with /api (for paths like "thumbnails/xxx.jpg")
    return `/api/${path}`;
  };

  const thumbnailUrl = getThumbnailUrl(board.thumbnailPath);

  // Keyboard accessibility
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleClick();
    }
  };

  return (
    <div
      className={styles.boardContainer}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      role="button"
      tabIndex={0}
      aria-label={`게시글 보기: ${board.title}`}
      style={{ cursor: 'pointer' }}
    >
      {/* Header */}
      <div className={styles.header}>
        <h1 className={styles.title}>{board.title}</h1>
        <button
          type="button"
          className={styles.writeButton}
          onClick={(e) => e.stopPropagation()}
          aria-label={`작성자: ${board.author.nickname}`}
        >
          {board.author.nickname}
        </button>
      </div>

      {/* Image or Status Badge */}
      <div className={styles.imageContainer}>
        {thumbnailUrl ? (
          <img
            src={thumbnailUrl}
            alt={board.title}
            className={styles.postImage}
            onError={(e) => {
              // Fallback on image load error
              const target = e.target as HTMLImageElement;
              target.style.display = 'none';
              const parent = target.parentElement;
              if (parent) {
                parent.innerHTML = `
                  <div class="${styles.placeholder}">
                    <div class="${styles.statusBadge}">
                      <span>이미지 불러오기 실패</span>
                    </div>
                  </div>
                `;
              }
            }}
          />
        ) : (
          <div className={styles.placeholder}>
            {board.status === 'PROCESSING' || board.status === 'CONVERTING' ? (
              <div className={styles.statusBadge}>
                <span>변환 중...</span>
                {board.progress && <span>{board.progress}%</span>}
              </div>
            ) : board.status === 'FAILED' ? (
              <div className={styles.statusBadge + ' ' + styles.error}>변환 실패</div>
            ) : board.thumbnailStatus === 'PENDING' ? (
              <div className={styles.statusBadge}>썸네일 생성 중...</div>
            ) : (
              <div className={styles.statusBadge}>이미지 없음</div>
            )}
          </div>
        )}
      </div>

      {/* Category & Content */}
      <div className={styles.content}>
        <span className={styles.category}>{categoryToLabel(board.category)}</span>
        {/* Note: API doesn't return content in summary, using title as fallback */}
        <p className={styles.contentText}>{truncateContent(board.title)}</p>
      </div>

      {/* Stats */}
      <div className={styles.stats}>
        <span className={styles.date}>{formatDate(board.createdAt)}</span>
        <div className={styles.statsRight}>
          <div className={styles.statItem}>
            <span className={styles.statIcon}>
              <img src={thumbIcon} alt="thumb" />
            </span>
            <span className={styles.statCount}>{board.likeCount}</span>
          </div>
          <span className={styles.separator}>•</span>
          <div className={styles.statItem}>
            <span className={styles.statIcon}>
              <img src={commentIcon} alt="comment" />
            </span>
            <span className={styles.statCount}>{board.commentCount}</span>
          </div>
          <span className={styles.separator}>•</span>
          <div className={styles.statItem}>
            <span className={styles.statIcon}>
              <img src={eyeIcon} alt="eye" />
            </span>
            <span className={styles.statCount}>{board.viewCount}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
