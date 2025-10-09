import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { boardEndpoints, commentEndpoints } from '@/services/endpoints';
import { useAuth } from '@/hooks/useAuth';
import type { BoardDetail, Comment } from '@/types/api';
import { categoryToLabel } from '@/types/api';
import styles from './BoardDetailContent.module.css';
import processingIcon from '@/assets/progress.png';
import heartIcon from '@/assets/heart.svg';
import heartDefaultIcon from '@/assets/heart_default.svg';
import calendarIcon from '@/assets/calendar.svg';
import eyeIcon from '@/assets/eye.svg';
import commentIcon from '@/assets/comment.svg';
import speechIcon from '@/assets/speech.svg';
import refreshIcon from '@/assets/refresh.svg';
import replyIcon from '@/assets/reply.svg';

export default function BoardDetailContent() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated, isAdmin } = useAuth();

  const [board, setBoard] = useState<BoardDetail | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [newComment, setNewComment] = useState('');
  const [replyingTo, setReplyingTo] = useState<number | null>(null);
  const [replyContent, setReplyContent] = useState('');
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [editContent, setEditContent] = useState('');

  const [isSubmittingComment, setIsSubmittingComment] = useState(false);
  const [commentError, setCommentError] = useState<string | null>(null);

  // Fetch board detail
  const fetchBoard = useCallback(async () => {
    if (!id) {
      setError('게시글 ID가 없습니다.');
      setIsLoading(false);
      return;
    }

    try {
      const data = await boardEndpoints.detail(Number(id));
      setBoard(data);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : '게시글을 불러오는데 실패했습니다.');
      console.error('Failed to fetch board:', err);
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  // Fetch comments
  const fetchComments = useCallback(async () => {
    if (!id) return;

    try {
      const response = await commentEndpoints.list(Number(id));
      setComments(response.content);
    } catch (err) {
      console.error('Failed to fetch comments:', err);
    }
  }, [id]);

  // Initial fetch
  useEffect(() => {
    fetchBoard();
    fetchComments();
  }, [fetchBoard, fetchComments]);

  // Polling for PROCESSING/CONVERTING boards
  useEffect(() => {
    if (!board) return;

    const shouldPoll =
      board.status === 'PROCESSING' ||
      board.status === 'CONVERTING' ||
      board.status === 'PENDING' ||
      board.status === 'DISPATCHED' ||
      board.status === 'DOWNLOADING';

    if (shouldPoll) {
      const interval = setInterval(() => {
        fetchBoard();
      }, 5000); // Poll every 5 seconds

      return () => clearInterval(interval);
    }
  }, [board, fetchBoard]);

  // Handle like toggle
  const handleLike = async () => {
    if (!board || !isAuthenticated) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    try {
      const response = await boardEndpoints.toggleLike(board.id);
      setBoard({
        ...board,
        isLikedByCurrentUser: response.isLiked,
        likeCount: response.likeCount,
      });
    } catch (err) {
      console.error('Failed to toggle like:', err);
      alert('좋아요 처리에 실패했습니다.');
    }
  };

  // Handle board delete
  const handleBoardDelete = async () => {
    if (!board) return;

    const isOwner = user?.id === board.authorId;
    if (!isAdmin && !isOwner) {
      alert('삭제 권한이 없습니다.');
      return;
    }

    if (!confirm('정말로 이 게시글을 삭제하시겠습니까?')) {
      return;
    }

    try {
      await boardEndpoints.delete(board.id);
      alert('게시글이 삭제되었습니다.');
      navigate('/post');
    } catch (err) {
      console.error('Failed to delete board:', err);
      alert('게시글 삭제에 실패했습니다.');
    }
  };

  // Handle board edit
  const handleBoardEdit = () => {
    if (!board) return;

    const isOwner = user?.id === board.authorId;
    if (!isAdmin && !isOwner) {
      alert('수정 권한이 없습니다.');
      return;
    }

    navigate(`/post/${board.id}/edit`);
  };

  // Handle comment submit
  const handleCommentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim() || !board) return;

    if (!isAuthenticated) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    setIsSubmittingComment(true);
    setCommentError(null);

    try {
      await commentEndpoints.create(board.id, { content: newComment.trim() });
      setNewComment('');
      await fetchComments();
      await fetchBoard(); // Refresh to update comment count
    } catch (err) {
      setCommentError(err instanceof Error ? err.message : '댓글 작성에 실패했습니다.');
    } finally {
      setIsSubmittingComment(false);
    }
  };

  // Handle reply click
  const handleReplyClick = (commentId: number) => {
    setReplyingTo(replyingTo === commentId ? null : commentId);
    setReplyContent('');
  };

  // Handle reply submit
  const handleReplySubmit = async (e: React.FormEvent, parentId: number) => {
    e.preventDefault();
    if (!replyContent.trim() || !board) return;

    if (!isAuthenticated) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    setIsSubmittingComment(true);
    setCommentError(null);

    try {
      await commentEndpoints.create(board.id, {
        content: replyContent.trim(),
        parentId,
      });
      setReplyContent('');
      setReplyingTo(null);
      await fetchComments();
      await fetchBoard(); // Refresh to update comment count
    } catch (err) {
      setCommentError(err instanceof Error ? err.message : '답글 작성에 실패했습니다.');
    } finally {
      setIsSubmittingComment(false);
    }
  };

  // Handle comment edit
  const handleEditClick = (comment: Comment) => {
    setEditingCommentId(comment.id);
    setEditContent(comment.content);
  };

  const handleEditSubmit = async (commentId: number) => {
    if (!editContent.trim()) return;

    setIsSubmittingComment(true);
    setCommentError(null);

    try {
      await commentEndpoints.update(commentId, { content: editContent.trim() });
      setEditingCommentId(null);
      setEditContent('');
      await fetchComments();
    } catch (err) {
      setCommentError(err instanceof Error ? err.message : '댓글 수정에 실패했습니다.');
    } finally {
      setIsSubmittingComment(false);
    }
  };

  // Handle comment delete
  const handleDeleteClick = async (commentId: number) => {
    if (!window.confirm('댓글을 삭제하시겠습니까?')) return;

    try {
      await commentEndpoints.delete(commentId);
      await fetchComments();
      await fetchBoard(); // Refresh to update comment count
    } catch (err) {
      alert(err instanceof Error ? err.message : '댓글 삭제에 실패했습니다.');
    }
  };

  const handleCancelReply = () => {
    setReplyingTo(null);
    setReplyContent('');
  };

  const handleCancelEdit = () => {
    setEditingCommentId(null);
    setEditContent('');
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

  // Format relative time
  const formatRelativeTime = (dateString: string) => {
    const now = new Date();
    const date = new Date(dateString);
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return '방금 전';
    if (diffMins < 60) return `${diffMins}분 전`;
    if (diffHours < 24) return `${diffHours}시간 전`;
    if (diffDays < 7) return `${diffDays}일 전`;
    return formatDate(dateString);
  };

  // Group comments by parent
  const topLevelComments = comments.filter((c) => !c.parentId);
  const getReplies = (parentId: number) =>
    comments.filter((c) => c.parentId === parentId);

  // Render single comment
  const renderSingleComment = (comment: Comment, isReply: boolean = false) => {
    const isOwner = user?.id === comment.userId;
    const canModerate = isOwner || isAdmin;
    const isEditing = editingCommentId === comment.id;

    return (
      <div
        key={comment.id}
        className={`${styles.comment} ${isReply ? styles.replyComment : ''}`}
      >
        <div className={styles.commentHeader}>
          <span className={styles.commentAuthor}>{comment.userNickname}</span>
        </div>

        <div className={styles.commentContentContainer}>
          {isEditing ? (
            <div className={styles.editForm}>
              <textarea
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                className={styles.editInput}
                maxLength={500}
                autoFocus
              />
              <div className={styles.editFormButtons}>
                <button
                  type="button"
                  onClick={handleCancelEdit}
                  className={styles.cancelButton}
                  disabled={isSubmittingComment}
                >
                  취소
                </button>
                <button
                  type="button"
                  onClick={() => handleEditSubmit(comment.id)}
                  className={styles.replySubmitButton}
                  disabled={isSubmittingComment || !editContent.trim()}
                >
                  {isSubmittingComment ? '수정 중...' : '수정'}
                </button>
              </div>
            </div>
          ) : (
            <>
              <p className={styles.commentContent}>{comment.content}</p>
              {canModerate && (
                <div className={styles.commentActions}>
                  {isOwner && (
                    <button
                      type="button"
                      className={styles.commentButton}
                      onClick={() => handleEditClick(comment)}
                    >
                      수정
                    </button>
                  )}
                  <button
                    type="button"
                    className={styles.commentButton}
                    onClick={() => handleDeleteClick(comment.id)}
                  >
                    삭제
                  </button>
                </div>
              )}
            </>
          )}
        </div>

        <div className={styles.commentFooter}>
          {!isReply && (
            <button
              type="button"
              className={styles.replyButton}
              onClick={() => handleReplyClick(comment.id)}
            >
              <img src={replyIcon} alt="reply" />
              댓글달기
            </button>
          )}
          <span className={styles.commentDate}>
            {formatRelativeTime(comment.createdAt)}
          </span>
        </div>

        {/* Reply form */}
        {!isReply && replyingTo === comment.id && (
          <div className={styles.replyForm}>
            <form
              onSubmit={(e) => handleReplySubmit(e, comment.id)}
              className={styles.replyFormContainer}
            >
              <textarea
                value={replyContent}
                onChange={(e) => setReplyContent(e.target.value)}
                placeholder={`${comment.userNickname}님에게 답글 작성...`}
                className={styles.replyInput}
                maxLength={500}
                autoFocus
              />
              <div className={styles.replyFormButtons}>
                <button
                  type="button"
                  onClick={handleCancelReply}
                  className={styles.cancelButton}
                  disabled={isSubmittingComment}
                >
                  취소
                </button>
                <button
                  type="submit"
                  className={styles.replySubmitButton}
                  disabled={isSubmittingComment || !replyContent.trim()}
                >
                  {isSubmittingComment ? '작성 중...' : '답글 작성'}
                </button>
              </div>
            </form>
          </div>
        )}
      </div>
    );
  };

  if (isLoading) {
    return (
      <div className={styles.loadingContainer}>
        <div className={styles.spinner}></div>
        <p>게시글을 불러오는 중...</p>
      </div>
    );
  }

  if (error || !board) {
    return (
      <div className={styles.errorContainer}>
        <p>❌ {error || '게시글을 찾을 수 없습니다.'}</p>
        <button onClick={() => navigate('/post')} className={styles.retryButton}>
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  const isProcessing =
    board.status === 'PROCESSING' ||
    board.status === 'CONVERTING' ||
    board.status === 'PENDING' ||
    board.status === 'DISPATCHED' ||
    board.status === 'DOWNLOADING';

  return (
    <div className={styles.container}>
      {/* Processing section */}
      {isProcessing && (
        <div className={styles.processingSection}>
          <img src={processingIcon} alt="processing" />
          <div>
            <p className={styles.processingText}>3D 모델을 생성하는 중입니다</p>
            <p className={styles.processingSubtext}>
              {board.progress !== null && board.progress !== undefined
                ? `진행률: ${board.progress}%`
                : '변환이 완료되면 이메일로 안내를 받습니다'}
            </p>
          </div>
        </div>
      )}

      {/* Main content */}
      <div className={styles.mainContent}>
        {/* Header */}
        <div className={styles.header}>
          <div className={styles.titleContainer}>
            <div className={styles.heartContainer}>
              <img src={heartDefaultIcon} alt="heart default icon" />
              <span className={styles.heartCount}>{board.likeCount}</span>
            </div>
            <h1 className={styles.title}>{board.title}</h1>
          </div>

          <div className={styles.boardActions}>
            <button
              type="button"
              onClick={handleLike}
              className={styles.likeButton}
              disabled={!isAuthenticated}
              style={{
                opacity: board.isLikedByCurrentUser ? 1 : 0.6,
                fontWeight: board.isLikedByCurrentUser ? 'bold' : 'normal',
              }}
            >
              <img src={heartIcon} alt="heart icon" />
              <span>{board.isLikedByCurrentUser ? '좋아요 취소' : '좋아요'}</span>
            </button>

            {(user?.id === board.authorId || isAdmin) && (
              <>
                <button
                  type="button"
                  onClick={handleBoardEdit}
                  className={styles.editButton}
                >
                  수정
                </button>
                <button
                  type="button"
                  onClick={handleBoardDelete}
                  className={styles.deleteButton}
                >
                  삭제
                </button>
              </>
            )}
          </div>
        </div>

        {/* Author info */}
        <div className={styles.authorInfo}>
          <span className={styles.author}>{board.authorNickname}</span>
          <div className={styles.stats}>
            <span className={styles.calendarText}>
              <img src={calendarIcon} alt="calendar icon" />{' '}
              {formatDate(board.createdAt)} •
            </span>
            <span className={styles.commentText}>
              <img src={commentIcon} alt="comment icon" /> {board.commentCount} •
            </span>
            <span className={styles.eyeText}>
              <img src={eyeIcon} alt="eye icon" /> {board.viewCount}
            </span>
          </div>
        </div>

        {/* Category badge */}
        <div className={styles.categoryBadge}>{categoryToLabel(board.category)}</div>

        {/* Board content */}
        <div className={styles.content}>
          <textarea
            className={styles.contentText}
            value={board.content}
            readOnly
          />
        </div>
      </div>

      {/* Comment section */}
      <div className={styles.commentSection}>
        <div className={styles.commentTitleContainer}>
          <div className={styles.commentTitleLeft}>
            <img src={speechIcon} alt="speech icon" />
            <h3 className={styles.commentTitle}>댓글 {board.commentCount}</h3>
            <button
              type="button"
              onClick={fetchComments}
              className={styles.refreshButton}
              title="새로고침"
            >
              <img src={refreshIcon} alt="refresh icon" />
            </button>
          </div>
        </div>

        {commentError && (
          <div className={styles.commentError}>❌ {commentError}</div>
        )}

        {/* Comment list */}
        <div className={styles.commentList}>
          {topLevelComments.length === 0 ? (
            <div className={styles.emptyComments}>
              첫 댓글을 작성해보세요!
            </div>
          ) : (
            topLevelComments.map((comment) => (
              <div key={comment.id} className={styles.commentThread}>
                {/* Main comment */}
                {renderSingleComment(comment)}

                {/* Replies */}
                {getReplies(comment.id).length > 0 && (
                  <div className={styles.repliesContainer}>
                    {getReplies(comment.id).map((reply) =>
                      renderSingleComment(reply, true)
                    )}
                  </div>
                )}
              </div>
            ))
          )}
        </div>

        {/* Comment input */}
        <form onSubmit={handleCommentSubmit} className={styles.commentForm}>
          <textarea
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            placeholder={
              isAuthenticated
                ? '댓글을 입력하세요.'
                : '로그인 후 댓글을 작성할 수 있습니다.'
            }
            className={styles.commentInput}
            maxLength={500}
            disabled={!isAuthenticated || isSubmittingComment}
          />
          <div className={styles.commentFormFooter}>
            <span className={styles.charCount}>
              {newComment.length}/500
            </span>
            <button
              type="submit"
              className={styles.submitButton}
              disabled={!isAuthenticated || !newComment.trim() || isSubmittingComment}
            >
              {isSubmittingComment ? '작성 중...' : '작성'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
