import { useEffect, useRef, useState } from 'react';
import * as THREE from 'three';
import * as GaussianSplats3D from '@mkkellogg/gaussian-splats-3d';
import styles from './PlyViewer.module.css';

export default function PlyViewer({ plyPath }: { plyPath: string }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    if (!containerRef.current) return;

    // Convert server absolute path to nginx URL path
    const normalizedPath = plyPath.replace('/srv/memorylab/uploads/', '/uploads/');
    const plyUrl = `https://mlab.snowytiger.me${normalizedPath}`;

    console.log('Starting Gaussian Splatting load from:', plyUrl);

    // Create Three.js scene
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(
      75,
      containerRef.current.clientWidth / containerRef.current.clientHeight,
      0.1,
      1000
    );
    camera.position.set(0, 0, 0.5);

    const renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setSize(containerRef.current.clientWidth, containerRef.current.clientHeight);
    renderer.setPixelRatio(window.devicePixelRatio);
    containerRef.current.appendChild(renderer.domElement);

    // Create Gaussian Splat Viewer
    const viewer = new GaussianSplats3D.Viewer({
      cameraUp: [0, 1, 0],
      initialCameraPosition: [0, 0, 0.5],
      initialCameraLookAt: [0, 0, 0],
      sphericalHarmonicsDegree: 2,
      renderer: renderer,
      camera: camera,
      scene: scene,
    });

    // Load Gaussian Splat PLY
    viewer.addSplatScene(plyUrl, {
      progressiveLoad: true,
      onProgress: (percent: number) => {
        // Library returns 0-1 range, clamp to 0-100
        const progress = Math.min(100, Math.max(0, Math.round(percent * 100)));
        setProgress(progress);
        if (progress % 10 === 0 || progress === 100) {
          console.log('Loading Gaussian Splat:', progress + '%');
        }
      },
    })
      .then(() => {
        console.log('Gaussian Splat loaded successfully');
        setLoading(false);
        viewer.start();
      })
      .catch((err: Error) => {
        console.error('Error loading Gaussian Splat:', err);
        setError('3D 모델 로딩 실패');
        setLoading(false);
      });

    // Handle window resize
    const handleResize = () => {
      if (!containerRef.current) return;
      const width = containerRef.current.clientWidth;
      const height = containerRef.current.clientHeight;
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
      renderer.setSize(width, height);
    };
    window.addEventListener('resize', handleResize);

    // Cleanup
    return () => {
      window.removeEventListener('resize', handleResize);
      viewer.stop();
      viewer.dispose();
      renderer.dispose();
      if (containerRef.current?.contains(renderer.domElement)) {
        containerRef.current.removeChild(renderer.domElement);
      }
    };
  }, [plyPath]);

  return (
    <div className={styles.viewerContainer}>
      <div ref={containerRef} className={styles.canvas} />

      {loading && (
        <div className={styles.loadingOverlay}>
          <div className={styles.loadingContent}>
            <div className={styles.spinner}></div>
            <p>3D 모델 로딩 중... {progress}%</p>
          </div>
        </div>
      )}

      {error && (
        <div className={styles.errorOverlay}>
          <p>{error}</p>
        </div>
      )}

      {!loading && !error && (
        <div className={styles.controls}>
          <p>🖱️ 마우스로 회전 • 스크롤로 확대/축소</p>
        </div>
      )}
    </div>
  );
}
