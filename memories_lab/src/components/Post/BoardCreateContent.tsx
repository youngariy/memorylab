import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { boardEndpoints } from '@/services/endpoints';
import { categoryToEnum, visibilityToEnum } from '@/types/api';
import { useAuth } from '@/hooks/useAuth';
import styles from './BoardCreateContent.module.css';
import uploadIcon from '@/assets/upload.png';

export default function BoardCreateContent() {
  const [title, setTitle] = useState('');
  const [category, setCategory] = useState('장면');
  const [visibility, setVisibility] = useState('전체공개');
  const [content, setContent] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();
  const { isAuthenticated, isAdmin } = useAuth();

  const handleFileSelect = (file: File) => {
    const maxSize = 500 * 1024 * 1024; // 500MB in bytes

    if (file.type !== 'video/mp4') {
      setError('MP4 파일만 업로드 가능합니다.');
      return;
    }

    if (file.size > maxSize) {
      setError('파일 크기는 최대 500MB까지 지원합니다.');
      return;
    }

    setSelectedFile(file);
    setError(null);
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);

    const file = e.dataTransfer.files[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  const handleRemoveFile = () => {
    setSelectedFile(null);
    setError(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!isAuthenticated) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    if (!title.trim()) {
      setError('제목을 입력해주세요.');
      return;
    }

    if (!content.trim()) {
      setError('설명을 입력해주세요.');
      return;
    }

    setError(null);
    setIsUploading(true);
    setUploadProgress(0);

    try {
      // Map Korean labels to backend enums
      const categoryEnum = categoryToEnum(category);
      const visibilityEnum = visibilityToEnum(visibility);

      const boardData = {
        title: title.trim(),
        content: content.trim(),
        category: categoryEnum,
        visibility: visibilityEnum,
      };

      // Call API with progress tracking
      const createdBoard = await boardEndpoints.create(
        boardData,
        selectedFile || undefined,
        (progress) => {
          setUploadProgress(progress);
        }
      );

      // Navigate to the created board detail page
      navigate(`/post/${createdBoard.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : '게시글 등록에 실패했습니다.');
      setIsUploading(false);
      setUploadProgress(0);
    }
  };

  const handleCancel = () => {
    if (isUploading) {
      if (!window.confirm('업로드가 진행 중입니다. 정말 취소하시겠습니까?')) {
        return;
      }
    }
    navigate(-1);
  };

  return (
    <div className={styles.boardCreateContent}>
      <h1 className={styles.title}>새 글 작성</h1>

      {error && (
        <div className={styles.errorMessage}>
          ❌ {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className={styles.form}>
        {/* Video upload section */}
        <div className={styles.section}>
          <label className={styles.sectionTitle}>동영상 (선택)</label>
          <div
            className={`${styles.uploadArea} ${
              isDragOver ? styles.dragOver : ''
            } ${selectedFile ? styles.hasFile : ''}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={!selectedFile ? handleUploadClick : undefined}
            style={{ cursor: selectedFile ? 'default' : 'pointer' }}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".mp4,video/mp4"
              onChange={handleFileInputChange}
              className={styles.hiddenInput}
              disabled={isUploading}
            />

            {selectedFile ? (
              <div className={styles.fileInfo}>
                <div className={styles.fileName}>{selectedFile.name}</div>
                <div className={styles.fileSize}>
                  {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB
                </div>
                {!isUploading && (
                  <button
                    type="button"
                    onClick={handleRemoveFile}
                    className={styles.removeFileButton}
                  >
                    파일 제거
                  </button>
                )}
              </div>
            ) : (
              <>
                <div className={styles.uploadIcon}>
                  <img src={uploadIcon} alt="upload icon" />
                </div>
                <div className={styles.uploadText}>
                  여기에 MP4 파일을 드래그 앤 드롭하거나, 클릭하여 파일을
                  선택하세요
                </div>
                <div className={styles.uploadSubtext}>(최대 500MB)</div>
              </>
            )}
          </div>

          {/* Upload progress bar */}
          {isUploading && uploadProgress > 0 && (
            <div className={styles.progressContainer}>
              <div className={styles.progressBar}>
                <div
                  className={styles.progressFill}
                  style={{ width: `${uploadProgress}%` }}
                />
              </div>
              <div className={styles.progressText}>{uploadProgress}%</div>
            </div>
          )}
        </div>

        {/* Title section */}
        <div className={styles.section}>
          <label className={styles.sectionTitle}>제목 *</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="제목을 입력하세요"
            className={styles.titleInput}
            required
            disabled={isUploading}
            maxLength={100}
          />
        </div>

        {/* Category section */}
        <div className={styles.section}>
          <label className={styles.sectionTitle}>분류</label>
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className={styles.selectInput}
            disabled={isUploading}
          >
            <option value="장면">장면</option>
            <option value="물체">물체</option>
            {isAdmin && <option value="공지사항">공지사항</option>}
            <option value="문의하기">문의하기</option>
          </select>
        </div>

        {/* Visibility section */}
        <div className={styles.section}>
          <label className={styles.sectionTitle}>공개 범위</label>
          <select
            value={visibility}
            onChange={(e) => setVisibility(e.target.value)}
            className={styles.selectInput}
            disabled={isUploading}
          >
            <option value="전체공개">전체공개</option>
            <option value="비공개">비공개</option>
          </select>
        </div>

        {/* Description section */}
        <div className={styles.section}>
          <label className={styles.sectionTitle}>설명 *</label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="게시글 내용을 입력하세요"
            className={styles.descriptionInput}
            rows={6}
            required
            disabled={isUploading}
            maxLength={5000}
          />
          <div className={styles.charCount}>
            {content.length}/5000
          </div>
        </div>

        {/* Button section */}
        <div className={styles.buttonContainer}>
          <button
            type="button"
            onClick={handleCancel}
            className={styles.cancelButton}
            disabled={isUploading}
          >
            취소
          </button>
          <button
            type="submit"
            className={styles.submitButton}
            disabled={!title.trim() || !content.trim() || isUploading}
          >
            {isUploading
              ? uploadProgress < 100
                ? `업로드 중... ${uploadProgress}%`
                : '처리 중...'
              : '등록'}
          </button>
        </div>
      </form>
    </div>
  );
}
