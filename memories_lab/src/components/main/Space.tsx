import { motion } from 'framer-motion';
import styles from './Space.module.css';
import arrowRight from '@/assets/arrow_right.svg';
import spaceVideo from '@/assets/space_video.mp4';

function Space() {
  return (
    <section className={styles.space}>
      <video className={styles.backgroundVideo} autoPlay loop muted playsInline>
        <source src={spaceVideo} type="video/mp4" />
        {/* 우주/성운 영상 */}
      </video>
      <div className={styles.videoOverlay}></div>
      <div className={styles.container}>
        <motion.div
          className={styles.content}
          initial={{ y: 50, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ duration: 0.8, delay: 0.2 }}
        >
          <motion.span
            className={styles.subtitle}
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.4 }}
          >
            <span>PERSBOL</span>
            <img src={arrowRight} alt="arrowRight" />
          </motion.span>

          <motion.h1
            className={styles.title}
            initial={{ y: 30, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.8, delay: 0.6 }}
          >
            사진을 넘어서, 공간으로
          </motion.h1>

          <motion.p
            className={styles.description}
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.8 }}
          >
            사진 속 순간이 단순한 기록을 넘어
            <br />
            현실처럼 체험할 수 있는 3D 공간으로 재탄생합니다.
          </motion.p>
        </motion.div>
      </div>
    </section>
  );
}

export default Space;
