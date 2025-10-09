import { motion } from 'framer-motion';
import { useInView } from 'framer-motion';
import { useRef } from 'react';
import styles from './Feature.module.css';

function Feature() {
  const ref = useRef(null);
  const isInView = useInView(ref, { once: true, margin: '-100px' });

  return (
    <section className={styles.feature} ref={ref}>
      <div className={styles.backgroundImage}>
        <div className={styles.overlay} />

        <div className={styles.container}>
          <motion.div
            className={styles.content}
            initial={{ y: 50, opacity: 0 }}
            animate={isInView ? { y: 0, opacity: 1 } : { y: 50, opacity: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
          >
            <motion.h2
              className={styles.title}
              initial={{ y: 30, opacity: 0 }}
              animate={isInView ? { y: 0, opacity: 1 } : { y: 30, opacity: 0 }}
              transition={{ duration: 0.8, delay: 0.4 }}
            >
              추억을 3D로,
              <br className={styles.mobileBr} />
              현실처럼
            </motion.h2>

            <motion.p
              className={styles.description}
              initial={{ y: 20, opacity: 0 }}
              animate={isInView ? { y: 0, opacity: 1 } : { y: 20, opacity: 0 }}
              transition={{ duration: 0.6, delay: 0.6 }}
            >
              사진 속 순간이 단순한 기록을 넘어
              <br />
              현실처럼 체험할 수 있는 3D 공간으로 재탄생합니다.
            </motion.p>

            {/* PC용 버튼 - content 영역 안에 */}
            <motion.button
              className={styles.detailButton}
              initial={{ y: 20, opacity: 0 }}
              animate={isInView ? { y: 0, opacity: 1 } : { y: 20, opacity: 0 }}
              transition={{ duration: 0.6, delay: 0.8 }}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
            >
              자세히 보기
            </motion.button>
          </motion.div>

          <motion.div
            className={styles.infoBox}
            initial={{ x: 50, opacity: 0 }}
            animate={isInView ? { x: 0, opacity: 1 } : { x: 50, opacity: 0 }}
            transition={{ duration: 0.8, delay: 0.6 }}
          >
            <p className={styles.infoText}>
              추억의 한 장면을 올리세요. 단 한 번의 업로드로, 당신만의 특별한 3D
              세상이 열립니다.
              <br />
              AI가 달아내는 생생한 일몰 경험으로, 사진을 넘어 새로운 차원의
              감동을 느껴보세요
            </p>
          </motion.div>

          {/* 모바일 전용 하단 텍스트 */}
          <motion.div
            className={styles.mobileBottomText}
            initial={{ y: 30, opacity: 0 }}
            animate={isInView ? { y: 0, opacity: 1 } : { y: 30, opacity: 0 }}
            transition={{ duration: 0.8, delay: 0.8 }}
          >
            <p>
              추억의 한 장면을 올리세요. 단 한 번의 업로드로,
              <br />
              당신만의 특별한 3D 세상이 열립니다.
              <br />
              AI가 달아내는 생생한 일몰 경험으로, 사진을 넘어
              <br />
              새로운 차원의 감동을 느껴보세요
            </p>
          </motion.div>

          {/* 모바일 전용 버튼 */}
          <motion.button
            className={styles.mobileButton}
            initial={{ y: 20, opacity: 0 }}
            animate={isInView ? { y: 0, opacity: 1 } : { y: 20, opacity: 0 }}
            transition={{ duration: 0.6, delay: 1.0 }}
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
          >
            자세히 보기
          </motion.button>
        </div>
      </div>
    </section>
  );
}

export default Feature;
