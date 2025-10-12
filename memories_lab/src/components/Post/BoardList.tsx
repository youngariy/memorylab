import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { boardEndpoints } from '@/services/endpoints';
import type { BoardSummary } from '@/types/api';
import { BoardCardSkeleton } from '@/components/common/SkeletonLoader';
import EmptyState from '@/components/common/EmptyState';
import styles from './BoardList.module.css';
import Board from './Board';

interface BoardListProps {
  category: string;
  searchQuery: string;
  onTotalCountChange: (count: number) => void;
}

export default function BoardList({ category, searchQuery, onTotalCountChange }: BoardListProps) {
  const [boards, setBoards] = useState<BoardSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  // Reset page when category or search query changes
  useEffect(() => {
    setPage(0);
  }, [category, searchQuery]);

  useEffect(() => {
    fetchBoards();
  }, [page, category, searchQuery]);

  const fetchBoards = async () => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await boardEndpoints.list({
        page,
        size: 12,
        ...(category && { category })
      });
      setBoards(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '게시글을 불러오는데 실패했습니다.';
      setError(errorMessage);
      console.error('Failed to fetch boards:', err);
    } finally {
      setIsLoading(false);
    }
  };

  // Filter boards based on search query
  const filteredBoards = useMemo(() => {
    if (!searchQuery.trim()) {
      return boards;
    }

    const query = searchQuery.toLowerCase();
    return boards.filter(
      (board) =>
        board.title.toLowerCase().includes(query) ||
        board.author.nickname.toLowerCase().includes(query) ||
        board.tags?.toLowerCase().includes(query)
    );
  }, [boards, searchQuery]);

  // Update total count whenever filtered boards or totalElements change
  useEffect(() => {
    // 검색 중이면 필터링된 게시글 개수, 아니면 전체 게시글 개수
    if (searchQuery.trim()) {
      onTotalCountChange(filteredBoards.length);
    } else {
      onTotalCountChange(totalElements);
    }
  }, [filteredBoards, totalElements, searchQuery, onTotalCountChange]);

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setPage(newPage);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  if (isLoading) {
    return (
      <div className={styles.boardList}>
        {Array.from({ length: 6 }).map((_, index) => (
          <BoardCardSkeleton key={index} />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className={styles.errorContainer}>
        <p>❌ {error}</p>
        <button onClick={fetchBoards} className={styles.retryButton}>
          다시 시도
        </button>
      </div>
    );
  }

  if (boards.length === 0) {
    return (
      <EmptyState
        icon="📝"
        title="아직 게시글이 없습니다"
        description="첫 번째 게시글을 작성해보세요!"
        action={{
          label: '게시글 작성하기',
          onClick: () => navigate('/post/create'),
        }}
      />
    );
  }

  if (filteredBoards.length === 0 && searchQuery.trim()) {
    return (
      <EmptyState
        icon="🔍"
        title="검색 결과가 없습니다"
        description={`"${searchQuery}"에 대한 검색 결과를 찾을 수 없습니다.`}
      />
    );
  }

  return (
    <div>
      <div className={styles.boardList}>
        {filteredBoards.map((board) => (
          <Board key={board.id} board={board} />
        ))}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className={styles.pagination}>
          <button
            onClick={() => handlePageChange(page - 1)}
            disabled={page === 0}
            className={styles.pageButton}
          >
            이전
          </button>

          <div className={styles.pageNumbers}>
            {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
              let pageNum = i;
              if (totalPages > 5) {
                if (page < 3) {
                  pageNum = i;
                } else if (page > totalPages - 3) {
                  pageNum = totalPages - 5 + i;
                } else {
                  pageNum = page - 2 + i;
                }
              }
              return (
                <button
                  key={pageNum}
                  onClick={() => handlePageChange(pageNum)}
                  className={`${styles.pageNumber} ${page === pageNum ? styles.active : ''}`}
                >
                  {pageNum + 1}
                </button>
              );
            })}
          </div>

          <button
            onClick={() => handlePageChange(page + 1)}
            disabled={page === totalPages - 1}
            className={styles.pageButton}
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
