import { Routes, Route, Navigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { AuthProvider } from './contexts/AuthContext';
import { useAuth } from './hooks/useAuth';
import ScrollToTop from './components/ScrollToTop';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Post from './pages/Post';
import BoardDetail from './pages/BoardDetail';
import BoardCreate from './pages/BoardCreate';
import BoardEdit from './pages/BoardEdit';
import NotFound from './pages/NotFound';
import styles from './App.module.css';

// Protected Route Component
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <div>Loading...</div>;
  }

  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}

function AppRoutes() {
  return (
    <div className={styles.app}>
      <ScrollToTop />
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.3 }}
      >
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/post" element={<Post />} />
          <Route path="/post/:id" element={<BoardDetail />} />
          <Route
            path="/post/create"
            element={
              <ProtectedRoute>
                <BoardCreate />
              </ProtectedRoute>
            }
          />
          <Route
            path="/post/:id/edit"
            element={
              <ProtectedRoute>
                <BoardEdit />
              </ProtectedRoute>
            }
          />
          {/* 404 catch-all route */}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </motion.div>
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
}

export default App;
