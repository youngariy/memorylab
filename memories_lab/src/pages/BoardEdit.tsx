import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { boardEndpoints } from '@/services/endpoints';
import { useAuth } from '@/hooks/useAuth';
import { categoryToEnum, categoryToLabel, visibilityToEnum, visibilityToLabel, type Category, type Visibility } from '@/types/api';
import MobileNavigation from '@/components/main/MobileNavigation';
import Navigation from '@/components/main/Navigation';
import Breadcrumbs from '@/components/common/Breadcrumbs';
import styles from './BoardCreate.module.css';
import { useMobile } from '@/hooks/useMobile';

function BoardEdit() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isAdmin } = useAuth();
  const { isMobile } = useMobile();

  const [title, setTitle] = useState('');
  const [category, setCategory] = useState('자유');
  const [visibility, setVisibility] = useState('전체공개');
  const [content, setContent] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchBoard = async () => {
      if (!id) return;

      try {
        const board = await boardEndpoints.detail(parseInt(id));

        // Check permissions
        const isOwner = user?.id === board.authorId;
        if (!isAdmin && !isOwner) {
          alert('수정 권한이 없습니다.');
          navigate(`/post/${id}`);
          return;
        }

        setTitle(board.title);
        setCategory(categoryToLabel(board.category));
        setVisibility(visibilityToLabel(board.visibility));
        setContent(board.content);
      } catch (err) {
        console.error('Failed to fetch board:', err);
        setError('게시글을 불러오는데 실패했습니다.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchBoard();
  }, [id, user, isAdmin, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    if (!title.trim()) {
      setError('제목을 입력해주세요.');
      return;
    }

    if (!content.trim()) {
      setError('내용을 입력해주세요.');
      return;
    }

    setIsSaving(true);
    setError(null);

    try {
      await boardEndpoints.update(parseInt(id), {
        title: title.trim(),
        content: content.trim(),
        category: categoryToEnum(category),
        visibility: visibilityToEnum(visibility),
      });

      alert('게시글이 수정되었습니다.');
      navigate(`/post/${id}`);
    } catch (err) {
      console.error('Failed to update board:', err);
      setError('게시글 수정에 실패했습니다.');
    } finally {
      setIsSaving(false);
    }
  };

  const breadcrumbs = [
    { label: '홈', path: '/' },
    { label: '게시글', path: '/post' },
    { label: '게시글 수정' },
  ];

  if (isLoading) {
    return (
      <div className={styles.boardCreateContainer}>
        {isMobile ? <MobileNavigation /> : <Navigation />}
        <div className={styles.boardCreateContent}>
          <p>로딩 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.boardCreateContainer}>
      {isMobile ? <MobileNavigation /> : <Navigation />}
      <div className={styles.boardCreateContent}>
        <Breadcrumbs items={breadcrumbs} />

        <form onSubmit={handleSubmit} className={styles.form}>
          {error && <div className={styles.error}>{error}</div>}

          <div className={styles.formGroup}>
            <label htmlFor="title">제목</label>
            <input
              id="title"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="제목을 입력하세요"
              className={styles.input}
            />
          </div>

          <div className={styles.formRow}>
            <div className={styles.formGroup}>
              <label htmlFor="category">카테고리</label>
              <select
                id="category"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className={styles.select}
              >
                <option value="자유">자유</option>
                <option value="공지">공지</option>
                <option value="QNA">QNA</option>
              </select>
            </div>

            <div className={styles.formGroup}>
              <label htmlFor="visibility">공개 설정</label>
              <select
                id="visibility"
                value={visibility}
                onChange={(e) => setVisibility(e.target.value)}
                className={styles.select}
              >
                <option value="전체공개">전체공개</option>
                <option value="비공개">비공개</option>
              </select>
            </div>
          </div>

          <div className={styles.formGroup}>
            <label htmlFor="content">내용</label>
            <textarea
              id="content"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="내용을 입력하세요"
              className={styles.textarea}
              rows={15}
            />
          </div>

          <div className={styles.actions}>
            <button
              type="button"
              onClick={() => navigate(`/post/${id}`)}
              className={styles.cancelButton}
              disabled={isSaving}
            >
              취소
            </button>
            <button
              type="submit"
              className={styles.submitButton}
              disabled={isSaving}
            >
              {isSaving ? '수정 중...' : '수정하기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default BoardEdit;
