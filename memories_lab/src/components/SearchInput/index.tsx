import { useState } from 'react';
import { Search } from 'lucide-react';
import styles from './SearchInput.module.css';

interface SearchInputProps {
  placeholder?: string;
  onSearch?: (value: string) => void;
  className?: string;
}

export default function SearchInput({
  placeholder = '제목이나 내용을 검색해보세요',
  onSearch,
  className,
}: SearchInputProps) {
  const [value, setValue] = useState('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    setValue(newValue);
    onSearch?.(newValue);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch?.(value);
  };

  return (
    <form
      onSubmit={handleSubmit}
      className={`${styles.searchContainer} ${className || ''}`}
    >
      <div className={styles.searchInputWrapper}>
        <Search className={styles.searchIcon} size={20} />
        <input
          type="text"
          value={value}
          onChange={handleChange}
          placeholder={placeholder}
          className={styles.searchInput}
        />
      </div>
    </form>
  );
}
